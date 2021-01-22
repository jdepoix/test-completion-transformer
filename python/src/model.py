import math
import warnings
from argparse import ArgumentParser

import torch
import torch.nn as nn
import torch.nn.functional as F
from torch.optim.adamw import AdamW
from torch.optim.lr_scheduler import _LRScheduler

import pytorch_lightning as pl


class InverseSquareRootLR(_LRScheduler):
    def __init__(self, optimizer, warmup_steps, last_epoch=-1):
        if warmup_steps <= 0:
            raise ValueError('warmup_steps must be > 0')
        self._warmup_steps = warmup_steps
        self._lr_steps = [param_group['lr'] / warmup_steps for param_group in optimizer.param_groups]
        self._decay_factors = [
            param_group['lr'] * warmup_steps ** 0.5 for param_group in optimizer.param_groups
        ]

        super().__init__(optimizer, last_epoch)

    def get_lr(self):
        if not self._get_lr_called_within_step:
            warnings.warn("To get the last learning rate computed by the scheduler, "
                          "please use `get_last_lr()`.", UserWarning)

        if self.last_epoch < self._warmup_steps:
            return [self.last_epoch * lr_step for lr_step in self._lr_steps]
        else:
            return [decay_factor * self.last_epoch ** -0.5 for decay_factor in self._decay_factors]


class PositionalEncoding(pl.LightningModule):
    def __init__(self, features_size, max_len, dropout=0.1):
        super(PositionalEncoding, self).__init__()
        self.dropout = nn.Dropout(p=dropout)

        pe = torch.zeros(max_len, features_size)
        position = torch.arange(0, max_len, dtype=torch.float).unsqueeze(1)
        div_term = torch.exp(torch.arange(0, features_size, 2).float() * (-math.log(10000.0) / features_size))
        pe[:, 0::2] = torch.sin(position * div_term)
        pe[:, 1::2] = torch.cos(position * div_term)
        pe = pe.unsqueeze(0).transpose(0, 1)
        self.register_buffer('pe', pe)

    def forward(self, x):
        x = x + self.pe[:x.size(0), :]
        return self.dropout(x)


class LabelSmoothedCrossEntropy(nn.Module):
    def __init__(self, epsilon: float = 0.1, reduction='mean'):
        super().__init__()
        self.epsilon = epsilon
        self.reduction = reduction

    def forward(self, preds, target):
        n = preds.size()[-1]
        log_preds = F.log_softmax(preds, dim=-1)
        loss = self._reduce_loss(-log_preds.sum(dim=-1), self.reduction)
        nll = F.nll_loss(log_preds, target, reduction=self.reduction)
        return self._linear_combination(loss / n, nll, self.epsilon)

    def _linear_combination(self, x, y, epsilon):
        return epsilon * x + (1 - epsilon) * y

    def _reduce_loss(self, loss, reduction='mean'):
        return loss.mean() if reduction == 'mean' else loss.sum() if reduction == 'sum' else loss


class GwtSectionPredictionTransformer(pl.LightningModule):
    @staticmethod
    def add_model_specific_args(parent_parser):
        parser = ArgumentParser(parents=[parent_parser], add_help=False)
        parser.add_argument('--max_sequence_length', type=int, default=1024)
        parser.add_argument('--embedding_size', type=int, default=512)
        parser.add_argument('--learning_rate', type=float, default=1e-4)
        parser.add_argument('--num_attention_heads', type=int, default=8)
        parser.add_argument('--num_encoder_layers', type=int, default=6)
        parser.add_argument('--num_decoder_layers', type=int, default=6)
        parser.add_argument('--feedforward_dimensions', type=int, default=2048)
        parser.add_argument('--positional_encoding_dropout', type=float, default=0.1)
        parser.add_argument('--transformer_dropout', type=float, default=0.1)
        parser.add_argument('--lr_warmup_steps', type=int, default=4000)
        parser.add_argument('--optimize_on_smoothed_loss', type=bool, default=False)
        return parser

    def __init__(
        self,
        vocab_size,
        padding_token_idx,
        max_sequence_length,
        embedding_size,
        learning_rate,
        num_attention_heads,
        num_encoder_layers,
        num_decoder_layers,
        feedforward_dimensions,
        positional_encoding_dropout,
        transformer_dropout,
        lr_warmup_steps=None,
        optimize_on_smoothed_loss=False,
    ):
        super().__init__()
        self.max_sequence_length = max_sequence_length
        self.criterion = nn.CrossEntropyLoss(ignore_index=padding_token_idx)
        self.optimize_on_smoothed_loss = optimize_on_smoothed_loss
        self.label_smoothed_cross_entropy = LabelSmoothedCrossEntropy()
        self.learning_rate = learning_rate

        self.padding_token_idx = padding_token_idx
        self.embedding_size = embedding_size
        self._scale_factor = math.sqrt(self.embedding_size)

        self.embedding = nn.Embedding(vocab_size, embedding_size)
        self.positional_encoding = PositionalEncoding(embedding_size, max_sequence_length, positional_encoding_dropout)
        self.transformer = nn.Transformer(
            embedding_size,
            nhead=num_attention_heads,
            num_encoder_layers=num_encoder_layers,
            num_decoder_layers=num_decoder_layers,
            dim_feedforward=feedforward_dimensions,
            dropout=transformer_dropout
        )
        self.fully_connected_out = nn.Linear(embedding_size, vocab_size)
        self.lr_warmup_steps = lr_warmup_steps
        self.save_hyperparameters()

    def forward(self, src, target):
        embedded_src = self.positional_encoding(self.embedding(src) * self._scale_factor)
        embedded_target = self.positional_encoding(self.embedding(target) * self._scale_factor)

        src_key_padding_mask = self._generate_key_padding_mask(src)
        target_key_padding_mask = self._generate_key_padding_mask(target)

        transformer_out = self.transformer(
            embedded_src,
            embedded_target,
            src_key_padding_mask=src_key_padding_mask,
            tgt_mask=self.transformer.generate_square_subsequent_mask(target.shape[0]).to(src.device),
            tgt_key_padding_mask=target_key_padding_mask,
            memory_key_padding_mask=src_key_padding_mask.clone(),
        )

        return self.fully_connected_out(transformer_out)

    def _generate_key_padding_mask(self, data):
        return data.transpose(0, 1) == self.padding_token_idx

    def _get_forward_loss(self, batch):
        source, target = batch
        target_in, target_out = target[:-1, :], target[1:, :]
        output = self(source, target_in)
        loss_src = output.reshape(-1, output.shape[2])
        loss_trg = target_out.reshape(-1)
        return self.criterion(loss_src, loss_trg), self.label_smoothed_cross_entropy(loss_src, loss_trg)

    def training_step(self, batch, batch_idx):
        loss, label_smoothed_loss = self._get_forward_loss(batch)
        self.log('learning_rate', self.optimizers().param_groups[0]['lr'])
        self.log('train_loss', loss)
        self.log('label_smoothed_train_loss', label_smoothed_loss)
        return loss if not self.optimize_on_smoothed_loss else label_smoothed_loss

    def validation_step(self, batch, batch_idx):
        loss, label_smoothed_loss = self._get_forward_loss(batch)
        self.log('val_loss', loss)
        self.log('label_smoothed_val_loss', label_smoothed_loss)
        return loss if not self.optimize_on_smoothed_loss else label_smoothed_loss

    def test_step(self, batch, batch_idx):
        loss, label_smoothed_loss = self._get_forward_loss(batch)
        self.log('test_loss', loss)
        self.log('label_smoothed_test_loss', label_smoothed_loss)
        return loss if not self.optimize_on_smoothed_loss else label_smoothed_loss

    def configure_optimizers(self):
        optimizer = AdamW(self.parameters(), lr=self.learning_rate)
        if self.lr_warmup_steps is None:
            return optimizer

        scheduler = InverseSquareRootLR(optimizer, self.lr_warmup_steps)
        return (
            [optimizer],
            [
                {
                    'scheduler': scheduler,
                    'interval': 'step',
                    'frequency': 1,
                    'reduce_on_plateau': False,
                    'monitor': 'label_smoothed_val_loss' if self.optimize_on_smoothed_loss else 'val_loss',
                }
            ]
        )

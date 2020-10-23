import math
from argparse import ArgumentParser

import torch
import torch.nn as nn
from torch.optim.adam import Adam
from torch.nn import functional

import pytorch_lightning as pl


class PositionalEncoding(pl.LightningModule):
    def __init__(self, features_size, max_len, dropout=0.1):
        super(PositionalEncoding, self).__init__()
        self.dropout = nn.Dropout(p=dropout)

        pe = torch.zeros(max_len, features_size)
        position = torch.arange(0, max_len, dtype=torch.float).unsqueeze(1)
        div_term = torch.exp(torch.arange(0, features_size, 2).float() * (-math.log(10000.0) / features_size))
        pe[:, 0::2] = torch.sin(position * div_term)
        pe[:, 1::2] = torch.cos(position * div_term)
        pe = pe.unsqueeze(0).transpose(0, 1).to(self.device)
        self.register_buffer('pe', pe)

    def forward(self, x):
        x = x + self.pe[:x.size(0), :]
        return self.dropout(x)


class LabelSmoothingLoss(pl.LightningModule):
    def __init__(self, label_smoothing, tgt_vocab_size, ignore_index=-100):
        assert 0.0 < label_smoothing <= 1.0
        self.ignore_index = ignore_index
        super(LabelSmoothingLoss, self).__init__()

        smoothing_value = label_smoothing / (tgt_vocab_size - 2)
        one_hot = torch.full((tgt_vocab_size,), smoothing_value, device=self.device)
        one_hot[self.ignore_index] = 0
        self.register_buffer('one_hot', one_hot.unsqueeze(0))

        self.confidence = 1.0 - label_smoothing

    def forward(self, output, target):
        model_prob = self.one_hot.repeat(target.size(0), 1)
        model_prob.scatter_(1, target.unsqueeze(1), self.confidence)
        model_prob.masked_fill_((target == self.ignore_index).unsqueeze(1), 0)

        return functional.kl_div(output, model_prob, reduction='sum')


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
    ):
        super().__init__()
        self.max_sequence_length = max_sequence_length
        self.criterion = nn.CrossEntropyLoss(ignore_index=padding_token_idx)
        self.label_smoothed_criterion = LabelSmoothingLoss(.1, vocab_size, padding_token_idx)
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
        return self.criterion(loss_src, loss_trg), self.label_smoothed_criterion(loss_src, loss_trg)

    def training_step(self, batch, batch_idx):
        loss, label_smoothed_loss = self._get_forward_loss(batch)
        self.log('train_loss', loss)
        self.log('label_smoothed_train_loss', label_smoothed_loss)
        return loss

    def validation_step(self, batch, batch_idx):
        loss, label_smoothed_loss = self._get_forward_loss(batch)
        self.log('val_loss', loss)
        self.log('label_smoothed_val_loss', label_smoothed_loss)
        return loss

    def test_step(self, batch, batch_idx):
        loss, label_smoothed_loss = self._get_forward_loss(batch)
        self.log('test_loss', loss)
        self.log('label_smoothed_test_loss', label_smoothed_loss)
        return loss

    def configure_optimizers(self):
        return Adam(self.parameters(), lr=self.learning_rate)

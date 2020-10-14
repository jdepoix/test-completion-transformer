import math
from argparse import ArgumentParser

import torch
import torch.nn as nn
from torch.optim.adam import Adam

import pytorch_lightning as pl


class PositionalEncoding(nn.Module):
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
        self.criterion = nn.CrossEntropyLoss(ignore_index=padding_token_idx)
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
        return self.criterion(output.reshape(-1, output.shape[2]), target_out.reshape(-1))

    def training_step(self, batch, batch_idx):
        loss = self._get_forward_loss(batch)
        result = pl.TrainResult(loss)
        result.log('train_loss', loss)
        return result

    def validation_step(self, batch, batch_idx):
        # TODO use predictor for validation?
        loss = self._get_forward_loss(batch)
        result = pl.EvalResult(checkpoint_on=loss)
        result.log('eval_loss', loss)
        return result

    def test_step(self, batch, batch_idx):
        loss = self._get_forward_loss(batch)
        result = pl.EvalResult()
        result.log('test_loss', loss)
        return result

    def configure_optimizers(self):
        return Adam(self.parameters(), lr=self.learning_rate)

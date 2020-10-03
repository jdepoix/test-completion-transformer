import math

import torch
import torch.nn as nn


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


# TODO check if I need to do .to(device) everywhere...
class ThenPredictionTransformer(nn.Module):
    def __init__(
        self,
        vocab_size,
        padding_token_idx,
        max_sequence_length,
        embedding_size,
        # num_attention_heads,
        # num_encoder_layers,
        # num_decoder_layers,
        # feedforward_dimensions,
        # positional_encoding_dropout,
        # transformer_dropout,
    ):
        super().__init__()
        self.padding_token_idx = padding_token_idx
        self.embedding_size = embedding_size

        self.embedding = nn.Embedding(vocab_size, embedding_size)
        self.positional_encoding = PositionalEncoding(embedding_size, max_sequence_length)
        self.transformer = nn.Transformer(embedding_size)
        self.fully_connected_out = nn.Linear(embedding_size, vocab_size)

    def forward(self, src, target):
        embedded_src = self.positional_encoding(self.embedding(src) * math.sqrt(self.embedding_size))
        embedded_target = self.positional_encoding(self.embedding(target) * math.sqrt(self.embedding_size))

        # TODO generate masks
        src_key_padding_mask = self._generate_key_padding_mask(src)
        target_key_padding_mask = self._generate_key_padding_mask(target)

        transformer_out = self.transformer(
            embedded_src,
            embedded_target,
            src_key_padding_mask=src_key_padding_mask,
            tgt_mask=self.transformer.generate_square_subsequent_mask(target.shape[0]),
            tgt_key_padding_mask=target_key_padding_mask,
            memory_key_padding_mask=src_key_padding_mask,
        )

        return self.fully_connected_out(transformer_out)

    def _generate_key_padding_mask(self, data):
        return data.transpose(0, 1) == self.padding_token_idx

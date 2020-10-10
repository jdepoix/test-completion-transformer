from argparse import ArgumentParser

import pytorch_lightning as pl
from pytorch_lightning import loggers

from data import GwtDataModule
from model import GwtSectionPredictionTransformer


def get_parser():
    parser = ArgumentParser()
    parser.add_argument('--dataset_base_path', type=str, required=True)
    return parser


def train(args):
    data_module = GwtDataModule(
        args.batch_size,
        args.num_dataset_workers,
        f'{args.dataset_base_path}/bpe_ast_split/train.jsonl',
        f'{args.dataset_base_path}/bpe_ast_split/validate.jsonl',
        f'{args.dataset_base_path}/bpe_ast_split/test.jsonl',
        f'{args.dataset_base_path}/bpe_ast_vocab.txt',
    )

    model = GwtSectionPredictionTransformer(
        data_module.vocab.get_size(),
        data_module.vocab.get_index(data_module.vocab.PAD_TOKEN),
        args.max_sequence_length,
        args.embedding_size,
        args.learning_rate,
        args.num_attention_heads,
        args.num_encoder_layers,
        args.num_decoder_layers,
        args.feedforward_dimensions,
        args.positional_encoding_dropout,
        args.transformer_dropout,
    )

    logger = loggers.TensorBoardLogger('../lightning_logs')
    logger.log_hyperparams(args)
    trainer = pl.Trainer.from_argparse_args(args, logger=logger)
    trainer.fit(model, data_module)


if __name__ == '__main__':
    parser = get_parser()
    parser = GwtDataModule.add_dataset_specific_args(parser)
    parser = GwtSectionPredictionTransformer.add_model_specific_args(parser)
    parser = pl.Trainer.add_argparse_args(parser)

    train(parser.parse_args())

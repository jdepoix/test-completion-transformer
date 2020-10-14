from argparse import ArgumentParser

import pytorch_lightning as pl
from pytorch_lightning import loggers

from data import GwtDataModule
from model import GwtSectionPredictionTransformer


def get_parser():
    parser = ArgumentParser()
    parser.add_argument('--dataset_base_path', type=str, required=True)
    parser.add_argument('--tensorboard_dir', type=str, default='lightning_logs')
    return parser


def train(args):
    # TODO implement invalidate-dataset-cache param
    # TODO pin pytorch lightning version
    # TODO UserWarning: Could not log computational graph since the `model.example_input_array` attribute is not set or `input_array` was not given
    #   warnings.warn(*args, **kwargs)

    # TODO
    # Exception ignored in: <Finalize object, dead>
    # Traceback (most recent call last):
    #   File "/opt/conda/lib/python3.7/multiprocessing/util.py", line 201, in __call__
    #     res = self._callback(*self._args, **self._kwargs)
    #   File "/opt/conda/lib/python3.7/multiprocessing/synchronize.py", line 87, in _cleanup
    #     sem_unlink(name)
    # FileNotFoundError: [Errno 2] No such file or directory

    # TODO
    # RuntimeError: could not unlink the shared memory file

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

    logger = loggers.TensorBoardLogger(args.tensorboard_dir)
    logger.log_hyperparams(args)
    trainer = pl.Trainer.from_argparse_args(args, logger=logger)
    trainer.fit(model, data_module)


if __name__ == '__main__':
    parser = get_parser()
    parser = GwtDataModule.add_dataset_specific_args(parser)
    parser = GwtSectionPredictionTransformer.add_model_specific_args(parser)
    parser = pl.Trainer.add_argparse_args(parser)

    train(parser.parse_args())

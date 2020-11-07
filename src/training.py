import os

from argparse import ArgumentParser

import pytorch_lightning as pl
from pytorch_lightning import loggers
from pytorch_lightning import callbacks

from data import GwtDataModule
from model import GwtSectionPredictionTransformer


def get_parser():
    parser = ArgumentParser()
    parser.add_argument('--dataset_base_path', type=str, required=True)
    parser.add_argument('--tensorboard_dir', type=str, default='lightning_logs')
    parser.add_argument('--experiment_name', type=str, default='default')
    parser.add_argument('--version', type=str, default=None)
    parser.add_argument('--invalidate_line_caches', action='store_true')
    parser.add_argument('--split', type=str, default='bpe_ast_split')

    parser = GwtDataModule.add_dataset_specific_args(parser)
    parser = GwtSectionPredictionTransformer.add_model_specific_args(parser)
    parser = pl.Trainer.add_argparse_args(parser)
    return parser


def train(args, custom_callbacks=None):
    data_module = GwtDataModule(
        args.batch_size,
        args.num_dataset_workers,
        f'{args.dataset_base_path}/{args.split}/train.jsonl',
        f'{args.dataset_base_path}/{args.split}/validate.jsonl',
        f'{args.dataset_base_path}/{args.split}/test.jsonl',
        f'{args.dataset_base_path}/bpe_ast_vocab.txt',
    )

    if args.invalidate_line_caches:
        data_module.invalidate_caches()

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
        args.lr_warmup_steps,
    )

    logger = loggers.TensorBoardLogger(
        args.tensorboard_dir,
        name=args.experiment_name,
        version=args.version,
    )
    logger.log_hyperparams(args)

    checkpoint_dir = os.path.join(logger.log_dir, 'checkpoints')

    trainer = pl.Trainer.from_argparse_args(
        args,
        resume_from_checkpoint=load_checkpoint_if_available(checkpoint_dir),
        logger=logger,
        checkpoint_callback=callbacks.ModelCheckpoint(
            filepath=f'{checkpoint_dir}/{{epoch}}-{{val_loss}}',
            save_top_k=5,
            monitor='val_loss',
            mode='min',
        ),
        **({'callbacks': custom_callbacks} if custom_callbacks else {}),
    )

    trainer.fit(model, data_module)
    return trainer


def load_checkpoint_if_available(checkpoint_dir):
    if not os.path.exists(checkpoint_dir):
        return None

    last_checkpoint = None
    for checkpoint in os.scandir(checkpoint_dir):
        if not checkpoint.name.endswith('.ckpt'):
            continue

        metrics = checkpoint.name.split('-')
        if len(metrics) != 2 or not metrics[0].startswith('epoch='):
            continue

        epoch = int(metrics[0].replace('epoch=', ''))
        if last_checkpoint is None or last_checkpoint['epoch'] < epoch:
            last_checkpoint = {
                'path': checkpoint.path,
                'epoch': epoch
            }

    return last_checkpoint['path'] if last_checkpoint else None


if __name__ == '__main__':
    train(get_parser().parse_args())

import os

import optuna
from optuna.integration import PyTorchLightningPruningCallback

import torch

import pytorch_lightning as pl
from pytorch_lightning.callbacks import EarlyStopping

from training import train, get_parser


def find_best_checkpoint(log_dir):
    scores = []
    for checkpoint in os.scandir(f'{log_dir}/checkpoints'):
        if checkpoint.name.endswith('.ckpt') and 'tmp' not in checkpoint.name:
            scores.append(
                torch.load(
                    checkpoint.path,
                    map_location=torch.device('cpu')
                )['callbacks'][pl.callbacks.model_checkpoint.ModelCheckpoint]['best_model_score']
            )
    return min(scores).item()


def objective(trial):
    relevant_metric = 'val_loss'

    args = get_parser().parse_args()
    args.experiment_name = 'hyperparameter_tuning'

    args.learning_rate = trial.suggest_categorical('learning_rate', [1e-3, 1e-4, 1e-5])
    args.lr_warmup_steps = trial.suggest_categorical('lr_warmup_steps', [500, 1000, 2000])
    args.accumulate_grad_batches = trial.suggest_categorical('accumulate_grad_batches', [16, 32, 64, 128])
    args.positional_encoding_dropout = trial.suggest_uniform('positional_encoding_dropout', 0.1, 0.5)
    args.transformer_dropout = trial.suggest_uniform('transformer_dropout', 0.1, 0.5)

    trainer = train(
        args,
        custom_callbacks=[
            PyTorchLightningPruningCallback(trial, monitor=relevant_metric),
            EarlyStopping(
                monitor=relevant_metric,
                min_delta=0.00,
                patience=3,
                verbose=False,
                mode='min',
            ),
        ],
    )

    return find_best_checkpoint(trainer.logger.log_dir)


def run_study():
    study = optuna.create_study(direction='minimize')
    study.optimize(objective, n_trials=50, timeout=24 * 60 * 60)


if __name__ == '__main__':
    run_study()

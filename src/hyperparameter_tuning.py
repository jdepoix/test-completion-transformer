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

    args.learning_rate = trial.suggest_categorical('learning_rate', [1e-2, 5e-3, 1e-3])
    args.lr_warmup_steps = trial.suggest_categorical('lr_warmup_steps', [1000, 2000, 4000])
    args.accumulate_grad_batches = trial.suggest_categorical('accumulate_grad_batches', [32, 64, 128])
    args.positional_encoding_dropout = trial.suggest_uniform('positional_encoding_dropout', 0.1, 0.3)
    args.transformer_dropout = trial.suggest_uniform('transformer_dropout', 0.1, 0.3)

    args.version = (
        f'lr{args.learning_rate}'
        f'_ws{args.lr_warmup_steps}'
        f'_agb{args.accumulate_grad_batches}'
        f'_pd{args.positional_encoding_dropout}'
        f'_td{args.transformer_dropout}'
    )

    trainer = train(
        args,
        custom_callbacks=[
            PyTorchLightningPruningCallback(trial, monitor=relevant_metric),
            EarlyStopping(
                monitor=relevant_metric,
                min_delta=0.5,
                patience=3,
                verbose=False,
                mode='min',
            ),
        ],
    )

    return find_best_checkpoint(trainer.logger.log_dir)


def run_study():
    study = optuna.create_study(
        study_name='hyperparameter_tuning_v2',
        storage='sqlite:///../optuna_cache.db',
        load_if_exists=True,
        direction='minimize',
    )
    study.optimize(objective, n_trials=50, timeout=24 * 60 * 60, catch=(Exception,))
    print(study.trials_dataframe())


if __name__ == '__main__':
    run_study()

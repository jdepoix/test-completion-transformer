import optuna
from optuna.integration import PyTorchLightningPruningCallback

import pytorch_lightning as pl

from training import train, get_parser


class MostRecentMetricCallback(pl.Callback):
    def __init__(self, metric_name):
        super().__init__()
        self._metric_name = metric_name
        self.most_recent_metrics = None

    def on_validation_end(self, trainer, pl_module):
        metric = trainer.callback_metrics.get(self._metric_name)
        if metric:
            self.most_recent_metrics = metric.item()


def objective(trial):
    relevant_metric = 'val_loss'

    args = get_parser().parse_args()
    args.experiment_name = 'hyperparameter_tuning'

    args.learning_rate = trial.suggest_categorical('learning_rate', [1e-3, 1e-4, 1e-5])
    args.embedding_size = trial.suggest_categorical('embedding_size', [512, 1024])
    args.num_attention_heads = trial.suggest_categorical('num_attention_heads', [4, 8, 16])
    args.num_encoder_layers = trial.suggest_categorical('num_encoder_layers', [4, 6, 8, 10])
    args.num_decoder_layers = trial.suggest_categorical('num_decoder_layers', [4, 6, 8, 10])
    args.feedforward_dimensions = trial.suggest_categorical('feedforward_dimensions', [512, 1024, 2048])
    args.positional_encoding_dropout = trial.suggest_uniform('positional_encoding_dropout', 0.1, 0.5)
    args.transformer_dropout = trial.suggest_uniform('transformer_dropout', 0.1, 0.5)

    metric_callback = MostRecentMetricCallback(relevant_metric)
    train(args, custom_callbacks=[metric_callback, PyTorchLightningPruningCallback(trial, monitor=relevant_metric)])

    if metric_callback.most_recent_metrics is None:
        raise ValueError('most recent metric should not be None by this point')

    return metric_callback.most_recent_metrics


def run_study():
    study = optuna.create_study(direction='minimize')
    study.optimize(objective, n_trials=100, timeout=24 * 60 * 60)


if __name__ == '__main__':
    run_study()

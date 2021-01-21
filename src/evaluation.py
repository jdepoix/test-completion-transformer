import os
import json
import traceback
import datetime
from multiprocessing.pool import Pool
import multiprocessing
from argparse import ArgumentParser

import torch
from torch.utils import tensorboard

import pytorch_lightning as pl

import javalang

from nltk.translate.bleu_score import corpus_bleu

from rouge_metric import PyRouge as Rouge

import bpe
import data
import sampling
from ast_sequentialization_api_client import AstSequentializationApiClient
from model import GwtSectionPredictionTransformer
from predict import PredictionPipeline, ThenSectionPredictor, FailedPrediction
from source_code import AstSequenceProcessor, TokenizedCodeProcessor


class Evaluator():
    def __init__(
        self,
        model_class,
        sampler_settings,
        dataset_path,
        vocab,
        bpe_processor,
        source_code_processor,
        max_prediction_length,
        num_workers,
        device,
        write_results_to_tensorboard,
        prediction_log_dir,
        log_interval=1000,
        dataset_ids_path=None,
    ):
        self._model_class = model_class
        self._sampler_settings = sampler_settings
        self._dataset_path = dataset_path
        self._vocab = vocab
        self._bpe_processor = bpe_processor
        self._source_code_processor = source_code_processor
        self._max_prediction_length = max_prediction_length
        self._num_worker = num_workers
        self._devices = [device] if device == 'cpu' else [
            torch.device(f'cuda:{device_id}') for device_id in range(torch.cuda.device_count())
        ]
        if device == 'cuda':
            torch.multiprocessing.set_start_method('spawn')
        self._write_results_to_tensorboard = write_results_to_tensorboard
        self._prediction_log_dir = prediction_log_dir
        self._log_interval = log_interval
        self._dataset_ids = None
        if dataset_ids_path is not None:
            with open(dataset_ids_path) as dataset_ids_file:
                self._dataset_ids = [line[:-1] for line in dataset_ids_file.readlines()]

    def evaluate(self, tensorboard_log_dir, max_number_of_checkpoints):
        for checkpoint in self._find_relevant_checkpoints(tensorboard_log_dir, max_number_of_checkpoints):
            print(f'[{datetime.datetime.now()}] {"=" * 30} {checkpoint} {"=" * 30}')
            results = self._evaluate_checkpoint(checkpoint)
            print(f'[{datetime.datetime.now()}] {results}')
            if self._write_results_to_tensorboard:
                self._report_results(tensorboard_log_dir, checkpoint, results)

    def _find_relevant_checkpoints(self, log_dir, max_number_of_checkpoints):
        scores = []
        for checkpoint in os.scandir(f'{log_dir}/checkpoints'):
            if checkpoint.name.endswith('.ckpt') and 'tmp' not in checkpoint.name:
                checkpoint_data = torch.load(
                    checkpoint.path,
                    map_location=torch.device('cpu')
                )['callbacks'][pl.callbacks.model_checkpoint.ModelCheckpoint]
                scores.append((
                    checkpoint_data['best_model_score'].item(),
                    checkpoint_data['best_model_path'],
                ))
        return [item[1] for item in sorted(scores, key=lambda a: a[0])[:max_number_of_checkpoints]]

    def _init_prediction_pipeline(self, checkpoint_path):
        process_id = int(multiprocessing.current_process().name.split('-')[-1])
        device = self._devices[process_id % len(self._devices)]
        Evaluator.prediction_pipeline = PredictionPipeline(
            ThenSectionPredictor(
                self._model_class.load_from_checkpoint(
                    checkpoint_path,
                    strict=False,
                ).to(
                    device
                ).eval(),
                self._vocab.get_index(self._vocab.SOS_TOKEN),
                self._vocab.get_index(self._vocab.EOS_TOKEN),
                self._max_prediction_length,
            ),
            self._source_code_processor,
            self._bpe_processor,
            self._vocab,
        )

    def _load_dataset(self):
        with open(self._dataset_path) as dataset_file:
            return list(enumerate(dataset_file))

    def _evaluate_checkpoint(self, checkpoint_path):
        with Pool(
            self._num_worker,
            initializer=self._init_prediction_pipeline,
            initargs=(checkpoint_path,)
        ) as pool:
            results_per_sampler = {}
            dataset = self._load_dataset()
            index = 0
            for sampler in self._sampler_settings:
                arg_list = []
                for datapoint_index, json_line in dataset:
                    arg_list.append((json_line, sampler, index, datapoint_index))
                    index += 1
                results_per_sampler[sampler] = pool.map(self._evaluate_datapoint, arg_list)

            return {
                sampler: self._process_results(results, sampler)
                for sampler, results in results_per_sampler.items()
            }

    def _evaluate_datapoint(self, args):
        try:
            json_line, sampler, index, datapoint_index = args
            source, target = json.loads(json_line)
            sampler_setting = self._sampler_settings[sampler]
            prediction = Evaluator.prediction_pipeline.execute_on_encoded(
                source,
                sampler=sampling.Loader(self._vocab).load_sampler(sampler_setting['type'], **sampler_setting['kwargs'])
            )
            tokenized_prediction = [token.value for token in javalang.tokenizer.tokenize(prediction)]
            if index % self._log_interval == 0:
                print(f'[{datetime.datetime.now()}] FINISHED evaluating {index}')
            return {
                'prediction': tokenized_prediction,
                'target': target,
                'datapoint_index': datapoint_index,
            }
        except Exception as exception:
            return {
                'exception': exception,
            }

    def _process_results(self, results, sampler):
        total_count = 0
        max_length_exceeded_count = 0
        contains_unknown_token_count = 0
        unparsable_count = 0
        error_count = 0

        predictions = []
        targets = []

        with \
                open(f'{self._prediction_log_dir}/{sampler}_predictions.log', 'w+') as predictions_log_file, \
                open(
                    f'{self._prediction_log_dir}/{sampler}_predictions_FAILED.log', 'w+'
                ) as failed_predictions_log_file, \
                open(f'{self._prediction_log_dir}/{sampler}_targets.log', 'w+') as targets_log_file:
            for index, result in enumerate(results):
                try:
                    if 'exception' in result:
                        raise result['exception']

                    predictions.append(result['prediction'])
                    targets.append([result['target']])
                    datapoint_id = result['datapoint_index'] \
                        if self._dataset_ids is None else self._dataset_ids[result['datapoint_index']]
                    predictions_log_file.write(f'{datapoint_id}\t{result["prediction"]}\n')
                    targets_log_file.write(f'{datapoint_id}\t{result["target"]}\n')
                except FailedPrediction as exception:
                    if isinstance(exception, PredictionPipeline.ContainsUnknownToken):
                        contains_unknown_token_count += 1
                    elif isinstance(exception, ThenSectionPredictor.PredictionExceededMaxLength):
                        max_length_exceeded_count += 1
                    elif isinstance(exception, PredictionPipeline.PredictionUnparsable):
                        unparsable_count += 1

                    try:
                        raw_prediction = self._vocab.decode(exception.raw_prediction)
                        if self._bpe_processor.UNKOWN_TOKEN not in raw_prediction:
                            raw_prediction = self._bpe_processor.decode(raw_prediction)
                    except Exception:
                        raw_prediction = exception.raw_prediction
                    try:
                        failed_predictions_log_file.write(f'{raw_prediction}\n')
                    except Exception:
                        pass
                except Exception:
                    print(f'[{datetime.datetime.now()}] evaluation for datapoint #{index} failed:')
                    traceback.print_exc()
                    error_count += 1
                finally:
                    total_count += 1

        print(f'[{datetime.datetime.now()}] finished processing results for {sampler}: ' + str({
            'total_count': total_count,
            'max_length_exceeded_count': max_length_exceeded_count,
            'contains_unknown_token_count': contains_unknown_token_count,
            'unparsable_count': unparsable_count,
            'error_count': error_count,
            'prediction_length': len(predictions),
            'target_length': len(targets),
        }))

        rouge_results = Rouge(rouge_n=(1, 2),).evaluate_tokenized(
            [[prediction] for prediction in predictions],
            [[target] for target in targets]
        )

        return {
            'max_length_exceeded_rate': max_length_exceeded_count / total_count * 100,
            'contains_unknown_token_rate': contains_unknown_token_count / total_count * 100,
            'unparsable_rate': unparsable_count / total_count * 100,
            'error_rate': error_count / total_count * 100,
            'bleu_score': corpus_bleu(targets, predictions) * 100,
            'bleu_score_n1': corpus_bleu(targets, predictions, weights=(1, 0, 0, 0,)) * 100,
            'bleu_score_n2': corpus_bleu(targets, predictions, weights=(0, 1, 0, 0,)) * 100,
            'bleu_score_n3': corpus_bleu(targets, predictions, weights=(0, 0, 1, 0,)) * 100,
            'bleu_score_n4': corpus_bleu(targets, predictions, weights=(0, 0, 0, 1,)) * 100,
            'rouge_l_precision': rouge_results['rouge-l']['p'] * 100,
            'rouge_l_recall': rouge_results['rouge-l']['r'] * 100,
            'rouge_l_fscore': rouge_results['rouge-l']['f'] * 100,
            'rouge_1_precision': rouge_results['rouge-1']['p'] * 100,
            'rouge_1_recall': rouge_results['rouge-1']['r'] * 100,
            'rouge_1_fscore': rouge_results['rouge-1']['f'] * 100,
            'rouge_2_precision': rouge_results['rouge-2']['p'] * 100,
            'rouge_2_recall': rouge_results['rouge-2']['r'] * 100,
            'rouge_2_fscore': rouge_results['rouge-2']['f'] * 100,
        }

    def _report_results(self, tensorboard_log_dir, checkpoint_path, results):
        step = self._get_step_from_checkpoint(checkpoint_path)
        with tensorboard.SummaryWriter(tensorboard_log_dir) as writer:
            for sampler, sampler_results in results.items():
                for metric, value in sampler_results.items():
                    writer.add_scalar(f'{sampler}__{metric}', value, global_step=step)

    def _get_step_from_checkpoint(self, checkpoint_path):
        return torch.load(checkpoint_path, map_location=torch.device('cpu'))['global_step']


def get_parser():
    parser = ArgumentParser()
    parser.add_argument(
        '--tensorboard_log_dir',
        type=str,
        required=True,
        help='The tensorboard dir resulting from training the model. '
             'The best performing models in the checkpoint subdir are selected for evaluation'
    )
    parser.add_argument(
        '--evaluation_dataset_path',
        type=str,
        required=True,
        help='The dataset to evaluate the model on.'
    )
    parser.add_argument(
        '--evaluation_dataset_ids_path',
        type=str,
        default=None,
        help='The file containing the IDs of the evaluateion dataset. '
             'If this is not provided the datapoints will be enumerated.'
    )
    parser.add_argument('--vocab_path', type=str, required=True, help='Path to the vocabulary.')
    parser.add_argument('--bpe_model_path', type=str, required=True, help='Path to the BPE model')
    parser.add_argument(
        '--num_workers',
        type=int,
        required=True,
        help='The number of processes to run the evaluation with'
    )
    parser.add_argument(
        '--prediction_log_dir',
        type=str,
        required=True,
        help='The path to log the prediction results to'
    )
    parser.add_argument(
        '--write_results_to_tensorboard',
        type=bool,
        default=False,
        help='whether results should be logged to tensorboard'
    )
    parser.add_argument('--sequentialization_api_port', type=int, default=5555)
    parser.add_argument('--sequentialization_api_host', type=str, default='localhost')
    parser.add_argument('--max_prediction_length', type=int, default=512)
    parser.add_argument(
        '--max_number_of_checkpoints',
        type=int,
        default=5,
        help='The number of best performing checkpoints in the tensorboard dir that should be evaluated.'
    )
    parser.add_argument(
        '--sampler_settings',
        nargs='+',
        type=str,
        default=[sampling.Type.GREEDY],
        help='Specify the sampler to use.'
    )
    parser.add_argument('--device', type=str, default='cuda', choices=('cpu', 'cuda',))
    parser.add_argument(
        '--format',
        type=str,
        default='AST',
        choices=('AST', 'CODE',),
        help='Specify whether the prediction pipeline should use AST or CODE encoding.'
    )
    parser.add_argument('--log_interval', type=int, default=1000)
    return parser


def parse_sampler_settings(sampler_args):
    samplers = {}
    for sampler_setting in sampler_args:
        if '?' in sampler_setting:
            sampler_type, kwargs_settings = sampler_setting.split('?')

            sampler_kwargs = {}
            for kwargs_setting in kwargs_settings.split('&'):
                key, value = kwargs_setting.split('=')
                sampler_kwargs[key] = float(value)

            samplers[sampler_setting] = {
                'type': sampler_type,
                'kwargs': sampler_kwargs,
            }
        else:
            samplers[sampler_setting] = {
                'type': sampler_setting,
                'kwargs': {},
            }
    return samplers


if __name__ == '__main__':
    args = get_parser().parse_args()

    os.makedirs(args.prediction_log_dir, exist_ok=True)

    sequentialization_client = AstSequentializationApiClient(
        args.sequentialization_api_host,
        args.sequentialization_api_port,
    )
    if args.format == 'AST':
        source_code_processor = AstSequenceProcessor(sequentialization_client)
    else:
        source_code_processor = TokenizedCodeProcessor(sequentialization_client)
    Evaluator(
        GwtSectionPredictionTransformer,
        parse_sampler_settings(args.sampler_settings),
        args.evaluation_dataset_path,
        data.Vocab(args.vocab_path),
        bpe.BpeProcessor(args.bpe_model_path),
        source_code_processor,
        args.max_prediction_length,
        args.num_workers,
        args.device,
        args.write_results_to_tensorboard,
        args.prediction_log_dir,
        args.log_interval,
        args.evaluation_dataset_ids_path,
    ).evaluate(args.tensorboard_log_dir, args.max_number_of_checkpoints)

import os
import json
import traceback
from collections import defaultdict
from concurrent.futures.process import ProcessPoolExecutor
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
from predict import PredictionPipeline, ThenSectionPredictor
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

    def evaluate(self, tensorboard_log_dir, max_number_of_checkpoints):
        for checkpoint in self._find_relevant_checkpoints(tensorboard_log_dir, max_number_of_checkpoints):
            print(f'{"=" * 30} {checkpoint} {"=" * 30}')
            results = self._evaluate_checkpoint(checkpoint)
            print(results)
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
                    checkpoint_path
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

    def _permute_dataset_with_samplers(self):
        with open(self._dataset_path) as dataset_file:
            index = 0
            for line in dataset_file:
                for sampler in self._sampler_settings:
                    yield index, line, sampler
                    index += 1

    def _evaluate_checkpoint(self, checkpoint_path):
        with ProcessPoolExecutor(
            self._num_worker,
            initializer=self._init_prediction_pipeline,
            initargs=(checkpoint_path,)
        ) as executor:
            futures_per_sampler = defaultdict(list)
            for index, json_line, sampler in self._permute_dataset_with_samplers():
                futures_per_sampler[sampler].append(
                    executor.submit(self._evaluate_datapoint, json_line, sampler, index)
                )

            return {
                sampler: self._process_futures(futures, sampler)
                for sampler, futures in futures_per_sampler.items()
            }

    def _evaluate_datapoint(self, json_line, sampler, index):
        source, target = json.loads(json_line)
        sampler_setting = self._sampler_settings[sampler]
        prediction = Evaluator.prediction_pipeline.execute_on_encoded(
            source,
            sampler=sampling.Loader(self._vocab).load_sampler(sampler_setting['type'], **sampler_setting['kwargs'])
        )
        tokenized_prediction = [token.value for token in javalang.tokenizer.tokenize(prediction)]
        if index % self._log_interval == 0:
            print(f'FINISHED evaluating {index}')
        return {
            'prediction': tokenized_prediction,
            'target': target,
        }

    def _process_futures(self, futures, sampler):
        total_count = 0
        max_length_exceeded_count = 0
        contains_unknown_token_count = 0
        unparsable_count = 0
        error_count = 0

        predictions = []
        targets = []

        with \
                open(f'{self._prediction_log_dir}/{sampler}_predictions.log', 'w+') as predictions_log_file, \
                open(f'{self._prediction_log_dir}/{sampler}_targets.log', 'w+') as targets_log_file:
            for index, future in enumerate(futures):
                try:
                    result = future.result()
                    predictions.append(result['prediction'])
                    targets.append([result['target']])
                    predictions_log_file.write(str(result['prediction']) + '\n')
                    targets_log_file.write(str(result['target']) + '\n')
                except PredictionPipeline.ContainsUnknownToken:
                    contains_unknown_token_count += 1
                except ThenSectionPredictor.PredictionExceededMaxLength:
                    max_length_exceeded_count += 1
                except AstSequentializationApiClient.ApiError:
                    unparsable_count += 1
                except Exception:
                    print(f'evaluation for datapoint #{index} failed:')
                    traceback.print_exc()
                    error_count += 1
                finally:
                    total_count += 1

        rouge_results = Rouge(rouge_n=(1, 2),).evaluate_tokenized(
            [[prediction] for prediction in predictions],
            [[target] for target in targets]
        )

        return {
            'max_length_exceeded_rate': max_length_exceeded_count / total_count * 100,
            'contains_unknown_token_rate': contains_unknown_token_count / total_count * 100,
            'unparsable_rate': unparsable_count / total_count * 100,
            'error_count': error_count,
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
    parser.add_argument('--tensorboard_log_dir', type=str, required=True)
    parser.add_argument('--evaluation_dataset_path', type=str, required=True)
    parser.add_argument('--vocab_path', type=str, required=True)
    parser.add_argument('--bpe_model_path', type=str, required=True)
    parser.add_argument('--num_workers', type=int, required=True)
    parser.add_argument('--prediction_log_dir', type=str, required=True)
    parser.add_argument('--write_results_to_tensorboard', type=bool, default=False)
    parser.add_argument('--sequentialization_api_port', type=int, default=5555)
    parser.add_argument('--sequentialization_api_host', type=str, default='localhost')
    parser.add_argument('--max_prediction_length', type=int, default=512)
    parser.add_argument('--max_number_of_checkpoints', type=int, default=5)
    parser.add_argument('--sampler_settings', nargs='+', type=str, default=[sampling.Type.GREEDY])
    parser.add_argument('--device', type=str, default='cuda', choices=('cpu', 'cuda',))
    parser.add_argument('--format', type=str, default='AST', choices=('AST', 'CODE',))
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
    ).evaluate(args.tensorboard_log_dir, args.max_number_of_checkpoints)

import os
import sys
import json
import traceback
from concurrent.futures.process import ProcessPoolExecutor
import multiprocessing

import torch
from torch.utils import tensorboard

import javalang

from nltk.translate.bleu_score import corpus_bleu

from rouge_metric import PyRouge as Rouge

import bpe
import data
from ast_sequentialization_api_client import AstSequentializationApiClient
from model import GwtSectionPredictionTransformer
from predict import PredictionPipeline, ThenSectionPredictor


class Evaluator():
    def __init__(
        self,
        model_class,
        dataset_path,
        vocab,
        bpe_processor,
        sequentialization_client,
        max_prediction_length,
        num_workers,
        device,
        log_interval=1000,
    ):
        self._model_class = model_class
        self._dataset_path = dataset_path
        self._vocab = vocab
        self._bpe_processor = bpe_processor
        self._sequentialization_client = sequentialization_client
        self._max_prediction_length = max_prediction_length
        self._num_worker = num_workers
        self._devices = [device] if device == 'cpu' else [
            torch.device(f'cuda:{device_id}') for device_id in range(torch.cuda.device_count())
        ]
        if device == 'cuda':
            torch.multiprocessing.set_start_method('spawn')
        self._log_interval = log_interval

    def evaluate(self, tensorboard_log_dir):
        with tensorboard.SummaryWriter(tensorboard_log_dir) as writer:
            for checkpoint in os.scandir(f'{tensorboard_log_dir}/checkpoints'):
                if checkpoint.path.endswith('.ckpt'):
                    print(f'{"=" * 30} {checkpoint.path} {"=" * 30}')
                    results = self._evaluate_checkpoint(checkpoint.path)
                    print(results)
                    self._report_results(writer, checkpoint.path, results)

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
            self._bpe_processor,
            self._vocab,
            self._sequentialization_client,
        )

    def _evaluate_checkpoint(self, checkpoint_path):
        with ProcessPoolExecutor(
            self._num_worker,
            initializer=self._init_prediction_pipeline,
            initargs=(checkpoint_path,)
        ) as executor:
            with open(self._dataset_path) as dataset_file:
                return self._process_futures([
                    executor.submit(self._evaluate_datapoint, json_line, index)
                    for index, json_line in enumerate(dataset_file)
                ])

    def _evaluate_datapoint(self, json_line, index):
        source, target = json.loads(json_line)
        prediction = Evaluator.prediction_pipeline.execute_on_encoded(source)
        tokenized_prediction = [token.value for token in javalang.tokenizer.tokenize(prediction)]
        if index % self._log_interval == 0:
            print(f'FINISHED evaluating {index}')
        return {
            'prediction': tokenized_prediction,
            'target': target,
        }

    def _process_futures(self, futures):
        total_count = 0
        max_length_exceeded_count = 0
        contains_unknown_token_count = 0
        unparsable_count = 0
        error_count = 0

        predictions = []
        targets = []

        for index, future in enumerate(futures):
            try:
                result = future.result()
                predictions.append(result['prediction'])
                targets.append([result['target']])
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

        rouge_results = Rouge(rouge_n=(2,)).evaluate_tokenized(
            [[prediction] for prediction in predictions],
            [[target] for target in targets]
        )

        return {
            'max_length_exceeded_rate': max_length_exceeded_count / total_count,
            'contains_unknown_token_rate': contains_unknown_token_count / total_count,
            'unparsable_rate': unparsable_count / total_count,
            'bleu_score': corpus_bleu(targets, predictions),
            'bleu_score_n1': corpus_bleu(targets, predictions, weights=(1, 0, 0, 0,)),
            'bleu_score_n2': corpus_bleu(targets, predictions, weights=(0, 1, 0, 0,)),
            'bleu_score_n3': corpus_bleu(targets, predictions, weights=(0, 0, 1, 0,)),
            'bleu_score_n4': corpus_bleu(targets, predictions, weights=(0, 0, 0, 1,)),
            'rouge_l_precision': rouge_results['rouge-l']['p'],
            'rouge_l_recall': rouge_results['rouge-l']['r'],
            'rouge_l_fscore': rouge_results['rouge-l']['f'],
            'rouge_2_precision': rouge_results['rouge-2']['p'],
            'rouge_2_recall': rouge_results['rouge-2']['r'],
            'rouge_2_fscore': rouge_results['rouge-2']['f'],
        }

    def _report_results(self, writer, checkpoint_path, results):
        step = self._get_step_from_checkpoint(checkpoint_path)
        for metric, value in results.items():
            writer.add_scalar(metric, value, global_step=step)

    def _get_step_from_checkpoint(self, checkpoint_path):
        return torch.load(checkpoint_path, map_location=torch.device('cpu'))['global_step']


if __name__ == '__main__':
    TENSORBOARD_LOG_DIR = sys.argv[1]
    EVALUATION_DATASET_PATH = sys.argv[2]
    VOCAB_PATH = sys.argv[3]
    BPE_MODEL_PATH = sys.argv[4]
    SEQUENTIALIZATION_API_PORT = int(sys.argv[5])
    NUM_WORKERS = int(sys.argv[6])
    DEVICE = sys.argv[7]
    LOG_INTERVAL = int(sys.argv[8])

    # TODO remove
    from DEPRECATED_model import GwtSectionPredictionTransformer
    Evaluator(
        GwtSectionPredictionTransformer,
        EVALUATION_DATASET_PATH,
        data.Vocab(VOCAB_PATH),
        bpe.BpeProcessor(BPE_MODEL_PATH),
        AstSequentializationApiClient('localhost', SEQUENTIALIZATION_API_PORT),
        512,
        NUM_WORKERS,
        DEVICE,
        LOG_INTERVAL,
    ).evaluate(TENSORBOARD_LOG_DIR)

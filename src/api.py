import os
import traceback

from flask import Flask, request, jsonify
from flask_cors import CORS

import torch

from ast_sequentialization_api_client import AstSequentializationApiClient
from model import GwtSectionPredictionTransformer
from data import Vocab
from bpe import BpeProcessor
from predict import PredictionPipeline, ThenSectionPredictor, NucleusSampler, GreedySampler

app = Flask(__name__)
cors = CORS(app)


class PredictionApi():
    SAMPLER = {
        'NUCLEUS': NucleusSampler(),
        'GREEDY': GreedySampler(),
    }

    class Status():
        SUCCESS = 'SUCCESS'
        ERROR = 'ERROR'

    def __init__(self, data_dir, model_dir, max_prediction_length):
        bpe_processor = BpeProcessor(f'{data_dir}/model/ast_values.model')
        vocab = Vocab(f'{data_dir}/data/bpe_ast_vocab.txt')
        sequentialization_client = AstSequentializationApiClient('localhost', 5555)
        device = 'cuda' if torch.cuda.is_available() else 'cpu'
        # TODO REMOVE!!!
        from DEPRECATED_model import GwtSectionPredictionTransformer
        self._predictors = {
            model_file.split('.ckpt')[0]: PredictionPipeline(
                ThenSectionPredictor(
                    GwtSectionPredictionTransformer.load_from_checkpoint(f'{model_dir}/{model_file}').to(device).eval(),
                    vocab.get_index(vocab.SOS_TOKEN),
                    vocab.get_index(vocab.EOS_TOKEN),
                    max_prediction_length,
                ),
                bpe_processor,
                vocab,
                sequentialization_client
            )
            for model_file in os.listdir(model_dir) if model_file.endswith('.ckpt')
        }

    def predict(
        self,
        model_name,
        test_file_content,
        test_class_name,
        test_method_signature,
        related_file_content,
        related_class_name,
        related_method_signature,
        then_section_start_index,
        sampler,
    ):
        try:
            return {
                'status': PredictionApi.Status.SUCCESS,
                'data': self._predictors[model_name].execute(
                    test_file_content,
                    test_class_name,
                    test_method_signature,
                    related_file_content,
                    related_class_name,
                    related_method_signature,
                    then_section_start_index,
                    self.SAMPLER.get(sampler),
                )
            }
        except Exception as exception:
            traceback.print_exc()
            return {
                'status': PredictionApi.Status.ERROR,
                'data': type(exception).__name__
            }


api = PredictionApi(os.environ['DATA_DIR'], os.environ['MODEL_DIR'], 512)


@app.route('/api/predictions/<model_name>', methods=('POST',))
def get_test_relation(model_name):
    data = request.json
    return jsonify(
        api.predict(
            model_name,
            data['testFileContent'],
            data['testClassName'],
            data['testMethodSignature'],
            data['relatedFileContent'],
            data['relatedClassName'],
            data['relatedMethodSignature'],
            data.get('thenSectionStartIndex'),
            data.get('sampler'),
        )
    )

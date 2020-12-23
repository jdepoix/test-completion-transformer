import torch

from sampling import NucleusSampler


class FailedPrediction(Exception):
    def __init__(self, raw_prediction):
        self.raw_prediction = raw_prediction


class ThenSectionPredictor():
    class PredictionExceededMaxLength(FailedPrediction):
        pass

    class InputSequenceExceededMaxLength(Exception):
        pass

    def __init__(
        self,
        model,
        sos_index,
        eos_index,
        max_length,
        default_sampler_initializer=lambda _: NucleusSampler()
    ):
        self._model = model
        self._sos_index = sos_index
        self._eos_index = eos_index
        self._max_length = max_length
        self._default_sampler_initializer = default_sampler_initializer

    def predict(self, test_declaration_sequence, sampler_initializer=None):
        if len(test_declaration_sequence) > self._model.max_sequence_length:
            raise ThenSectionPredictor.InputSequenceExceededMaxLength()

        if sampler_initializer is None:
            sampler = self._default_sampler_initializer(test_declaration_sequence)
        else:
            sampler = sampler_initializer(test_declaration_sequence)

        test_declaration_tensor = torch.tensor(test_declaration_sequence).to(self._model.device)
        prediction = [self._sos_index]
        while prediction[-1] != self._eos_index:
            if len(prediction) >= self._max_length:
                raise ThenSectionPredictor.PredictionExceededMaxLength(prediction[1:])
            prediction.append(
                sampler.sample(
                    self._forward(test_declaration_tensor, prediction),
                    previous_predictions=prediction,
                )
            )
        return prediction[1:-1]

    def _forward(self, source, previous_predictions):
        with torch.no_grad():
            target = torch.tensor(previous_predictions).to(source.device)
            output = self._model(source.unsqueeze(1), target.unsqueeze(1))
            return output[-1].squeeze()


class PredictionPipeline():
    class ContainsUnknownToken(FailedPrediction):
        pass

    class PredictionUnparsable(FailedPrediction):
        pass

    def __init__(self, predictor, source_code_processor, bpe_processor, vocab):
        self._predictor = predictor
        self._source_code_processor = source_code_processor
        self._bpe_processor = bpe_processor
        self._vocab = vocab

    def execute(
        self,
        test_file_content,
        test_class_name,
        test_method_signature,
        related_file_content,
        related_class_name,
        related_method_signature,
        then_section_start_index=None,
        sampler=None,
    ):
        test_declaration_sequence = self._source_code_processor.encode(
            test_file_content,
            test_class_name,
            test_method_signature,
            related_file_content,
            related_class_name,
            related_method_signature,
            then_section_start_index,
        )
        encoded_sequence = self._bpe_processor.encode(test_declaration_sequence)
        model_input = self._vocab.encode(encoded_sequence)
        return self.execute_on_encoded(model_input, sampler)

    def execute_on_encoded(self, encoded_sequence, sampler=None):
        prediction = self._predictor.predict(encoded_sequence, sampler_initializer=sampler)
        try:
            decoded_prediction = self._vocab.decode(prediction)
            if self._bpe_processor.UNKOWN_TOKEN in decoded_prediction:
                raise PredictionPipeline.ContainsUnknownToken(prediction)
            decoded_sequence = self._bpe_processor.decode(decoded_prediction)
            return self._source_code_processor.decode(decoded_sequence)
        except FailedPrediction as exception:
            raise exception
        except Exception:
            raise PredictionPipeline.PredictionUnparsable(prediction)

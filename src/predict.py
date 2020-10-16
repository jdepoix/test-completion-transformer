import torch


class ThenSectionPredictor():
    class MaxLengthExceeded(Exception):
        pass

    def __init__(self, model, sos_index, eos_index, max_length):
        self._model = model
        self._sos_index = sos_index
        self._eos_index = eos_index
        self._max_length = max_length

    def predict(self, test_declaration_sequence):
        # TODO use final output or construct sentence out of [-1] of each outputs?
        test_declaration_tensor = torch.tensor(test_declaration_sequence).to(self._model.device)
        prediction = [self._sos_index]
        while prediction[-1] != self._eos_index:
            if len(prediction) >= self._max_length:
                raise ThenSectionPredictor.MaxLengthExceeded()
            prediction.append(self._forward(test_declaration_tensor, prediction))
        return prediction[1:-1]

    def _forward(self, source, previous_predictions):
        with torch.no_grad():
            target = torch.tensor(previous_predictions).to(source.device)
            output = self._model(source.unsqueeze(1), target.unsqueeze(1))
            return output[-1].argmax().item()


class PredictionPipeline():
    class ContainsUnknownToken(Exception):
        pass

    def __init__(self, predictor, bpe_processor, vocab, sequentialization_client):
        self._predictor = predictor
        self._bpe_processor = bpe_processor
        self._vocab = vocab
        self._sequentialization_client = sequentialization_client

    def predict(
        self,
        test_file_content,
        test_class_name,
        test_method_signature,
        related_file_content,
        related_class_name,
        related_method_signature,
    ):
        test_declaration_sequence = self._sequentialization_client.create_test_declaration_sequence(
            test_file_content,
            test_class_name,
            test_method_signature,
            related_file_content,
            related_class_name,
            related_method_signature,
        )
        encoded_sequence = self._bpe_processor.encode(test_declaration_sequence)
        input = self._vocab.encode(encoded_sequence)
        prediction = self._predictor.predict(input)
        decoded_prediction = self._vocab.decode(prediction)
        if self._bpe_processor.UNKOWN_TOKEN in decoded_prediction:
            raise PredictionPipeline.ContainsUnknownToken()
        decoded_sequence = self._bpe_processor.decode(decoded_prediction)
        return self._sequentialization_client.parse_then_sequence_to_code(decoded_sequence)

import torch
import torch.nn.functional as F


class NucleusSampler():
    def __init__(self, top_p=0.9, filter_value=-float('Inf')):
        self._top_p = top_p
        self._filter_value = filter_value

    def sample(self, logits):
        assert logits.dim() == 1
        # https://gist.github.com/thomwolf/1a5a29f6962089e871b94cbd09daf317
        sorted_logits, sorted_indices = torch.sort(logits, descending=True)
        cumulative_probs = torch.cumsum(F.softmax(sorted_logits, dim=-1), dim=-1)

        # Remove tokens with cumulative probability above the threshold
        sorted_indices_to_remove = cumulative_probs > self._top_p
        # Shift the indices to the right to keep also the first token above the threshold
        sorted_indices_to_remove[..., 1:] = sorted_indices_to_remove[..., :-1].clone()
        sorted_indices_to_remove[..., 0] = 0

        indices_to_remove = sorted_indices[sorted_indices_to_remove]
        logits[indices_to_remove] = self._filter_value

        probabilities = F.softmax(logits, dim=-1)
        return torch.multinomial(probabilities, 1)


class GreedySampler():
    def sample(self, logits):
        return logits.argmax().item()


class ThenSectionPredictor():
    class PredictionExceededMaxLength(Exception):
        pass

    class InputSequenceExceededMaxLength(Exception):
        pass

    def __init__(self, model, sos_index, eos_index, max_length, sampler=NucleusSampler()):
        self._model = model
        self._sos_index = sos_index
        self._eos_index = eos_index
        self._max_length = max_length
        self._sampler = sampler

    def predict(self, test_declaration_sequence):
        if len(test_declaration_sequence) > self._model.max_sequence_length:
            raise ThenSectionPredictor.InputSequenceExceededMaxLength()

        test_declaration_tensor = torch.tensor(test_declaration_sequence).to(self._model.device)
        prediction = [self._sos_index]
        while prediction[-1] != self._eos_index:
            if len(prediction) >= self._max_length:
                raise ThenSectionPredictor.PredictionExceededMaxLength()
            prediction.append(self._sampler.sample(self._forward(test_declaration_tensor, prediction)))
        return prediction[1:-1]

    def _forward(self, source, previous_predictions):
        with torch.no_grad():
            target = torch.tensor(previous_predictions).to(source.device)
            output = self._model(source.unsqueeze(1), target.unsqueeze(1))
            return output[-1].squeeze()


class PredictionPipeline():
    class ContainsUnknownToken(Exception):
        pass

    def __init__(self, predictor, bpe_processor, vocab, sequentialization_client):
        self._predictor = predictor
        self._bpe_processor = bpe_processor
        self._vocab = vocab
        self._sequentialization_client = sequentialization_client

    def execute(
        self,
        test_file_content,
        test_class_name,
        test_method_signature,
        related_file_content,
        related_class_name,
        related_method_signature,
        then_section_start_index=None,
    ):
        test_declaration_sequence = self._sequentialization_client.create_test_declaration_sequence(
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
        return self.execute_on_encoded(model_input)

    def execute_on_encoded(self, encoded_sequence):
        prediction = self._predictor.predict(encoded_sequence)
        decoded_prediction = self._vocab.decode(prediction)
        if self._bpe_processor.UNKOWN_TOKEN in decoded_prediction:
            raise PredictionPipeline.ContainsUnknownToken()
        decoded_sequence = self._bpe_processor.decode(decoded_prediction)
        return self._sequentialization_client.parse_then_sequence_to_code(decoded_sequence)

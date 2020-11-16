from abc import ABC, abstractmethod

import torch
import torch.nn.functional as F

from ast_sequence import AstSequence, Token


class Sampler(ABC):
    @abstractmethod
    def sample(self, logits, **kwargs):
        pass


class NucleusSampler(Sampler):
    def __init__(self, top_p=0.95, filter_value=-float('Inf')):
        self._top_p = top_p
        self._filter_value = filter_value

    def sample(self, logits, **kwargs):
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
        return torch.multinomial(probabilities, 1).item()


class GreedySampler(Sampler):
    def sample(self, logits, **kwargs):
        return logits.argmax().item()


class OnlyKnownIdentifiersSampler(Sampler):
    def __init__(self, vocab, input_ast_sequence, base_sampler, fallback_p=1e-3, filter_value=0.0):
        # TODO should this consider token sequence instead of individual tokens?
        self._vocab = vocab
        self._known_identifiers = set(
            vocab.encode(AstSequence.get_identifier_tokens(vocab.decode(input_ast_sequence)))
            + [vocab.get_index(Token.IDENTIFIER_CLOSE)]
        )
        self._base_sampler = base_sampler
        self._fallback_p = fallback_p
        self._filter_value = filter_value

    def sample(self, logits, **kwargs):
        if (
            'previous_predictions' in kwargs
            and AstSequence.ends_with_open_identifier(self._vocab.decode(kwargs['previous_predictions']))
        ):
            known_logits = torch.tensor([
                logits[index] if logit > self._fallback_p and index in self._known_identifiers else self._filter_value
                for index, logit in enumerate(F.softmax(logits, dim=-1))
            ])
            if any(known_logits != self._filter_value):
                logits = known_logits

        return self._base_sampler.sample(logits)


class ThenSectionPredictor():
    class PredictionExceededMaxLength(Exception):
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
                raise ThenSectionPredictor.PredictionExceededMaxLength()
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
        sampler=None,
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
        return self.execute_on_encoded(model_input, sampler)

    def execute_on_encoded(self, encoded_sequence, sampler=None):
        prediction = self._predictor.predict(encoded_sequence, sampler_initializer=sampler)
        decoded_prediction = self._vocab.decode(prediction)
        if self._bpe_processor.UNKOWN_TOKEN in decoded_prediction:
            raise PredictionPipeline.ContainsUnknownToken()
        decoded_sequence = self._bpe_processor.decode(decoded_prediction)
        return self._sequentialization_client.parse_then_sequence_to_code(decoded_sequence)

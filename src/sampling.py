from abc import ABC, abstractmethod

import torch
from torch.nn import functional as F

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
                logits = F.softmax(known_logits, dim=-1)

        return self._base_sampler.sample(logits)


class Type:
    ONLY_KNOWN_IDENTIFIERS_NUCLEUS = 'ONLY_KNOWN_IDENTIFIERS_NUCLEUS'
    ONLY_KNOWN_IDENTIFIERS_GREEDY = 'ONLY_KNOWN_IDENTIFIERS_GREEDY'
    NUCLEUS = 'NUCLEUS'
    GREEDY = 'GREEDY'


class Loader():
    def __init__(self, vocab):
        self._vocab = vocab

    def load_sampler(self, sampler_type, **kwargs):
        nucleus_kwargs = {'p': kwargs['p']} if 'p' in kwargs else {}
        only_known_kwargs = {'fallback_p': kwargs['fallback_p']} if 'fallback_p' in kwargs else {}

        if sampler_type == Type.ONLY_KNOWN_IDENTIFIERS_NUCLEUS:
            return lambda seq: OnlyKnownIdentifiersSampler(
                self._vocab, seq, NucleusSampler(**nucleus_kwargs), **only_known_kwargs
            )
        if sampler_type == Type.ONLY_KNOWN_IDENTIFIERS_GREEDY:
            return lambda seq: OnlyKnownIdentifiersSampler(self._vocab, seq, GreedySampler(), **only_known_kwargs)
        if sampler_type == Type.NUCLEUS:
            return lambda _: NucleusSampler(**nucleus_kwargs)
        if sampler_type == Type.GREEDY:
            return lambda _: GreedySampler()

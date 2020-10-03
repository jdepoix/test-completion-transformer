import json

import torch
from torch.nn.utils.rnn import pad_sequence
from torch.utils.data import Dataset


class Vocab():
    PAD_TOKEN = '<[PAD]>'
    SOS_TOKEN = '<[THEN]>'
    EOS_TOKEN = '<[/THEN]>'

    def __init__(self, vocab_file_path):
        words = []
        indices = {}
        with open(vocab_file_path) as vocab_file:
            for index, line in enumerate(vocab_file):
                word = line[:-1]
                words.append(word)
                indices[word] = index
        self._words = words
        self._indices = indices

    def get_index(self, word):
        return self._indices[word]

    def get_word(self, index):
        return self._words[index]

    def get_size(self):
        return len(self._words)


class BatchPadder():
    def __init__(self, padding_token_idx):
        self._padding_token_idx = padding_token_idx

    def create_batch(self, batch):
        sources, targets = zip(*batch)
        return (
            pad_sequence(sources, batch_first=False, padding_value=self._padding_token_idx),
            pad_sequence(targets, batch_first=False, padding_value=self._padding_token_idx),
        )


class GwtDataset(Dataset):
    def __init__(self, dataset_path):
        # TODO should I really load this into memory???
        self._data = []
        with open(dataset_path) as dataset_file:
            for json_line in dataset_file:
                self._data.append(json.loads(json_line))

    def __len__(self):
        return len(self._data)

    def __getitem__(self, item):
        src, trg = self._data[item]
        return torch.tensor(src), torch.tensor(trg)

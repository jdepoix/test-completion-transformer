import json
from argparse import ArgumentParser

import torch
from pytorch_lightning import LightningDataModule
from torch.nn.utils.rnn import pad_sequence
from torch.utils.data import Dataset, DataLoader


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


class GwtDataModule(LightningDataModule):
    @staticmethod
    def add_dataset_specific_args(parent_parser):
        parser = ArgumentParser(parents=[parent_parser], add_help=False)
        parser.add_argument('--batch_size', type=int, default=32)
        parser.add_argument('--num_dataset_workers', type=int, default=8)
        return parser

    def __init__(
        self,
        batch_size,
        num_workers,
        train_dataset_path,
        validation_dataset_path,
        test_dataset_path,
        vocab_path,
    ):
        super().__init__()
        self._batch_size = batch_size
        self._num_worker = num_workers
        self._train_dataset_path = train_dataset_path
        self._validation_dataset_path = validation_dataset_path
        self._test_dataset_path = test_dataset_path
        self._vocab_path = vocab_path
        self._train_dataset = None
        self._validation_dataset = None
        self._test_dataset = None
        self._batch_padder = None
        self.vocab = Vocab(vocab_path)

    def setup(self, stage):
        if stage == 'fit':
            self._train_dataset = GwtDataset(self._train_dataset_path)
            self._validation_dataset = GwtDataset(self._validation_dataset_path)
        if stage == 'test':
            self._test_dataset = GwtDataset(self._test_dataset_path)
        self._batch_padder = BatchPadder(self.vocab.get_index(self.vocab.PAD_TOKEN))

    def train_dataloader(self):
        return DataLoader(
            self._train_dataset,
            batch_size=self._batch_size,
            collate_fn=self._batch_padder.create_batch,
            num_workers=self._num_worker,
        )

    def val_dataloader(self):
        return DataLoader(
            self._validation_dataset,
            batch_size=self._batch_size,
            collate_fn=self._batch_padder.create_batch,
            num_workers=self._num_worker,
        )

    def test_dataloader(self):
        return DataLoader(
            self._test_dataset,
            batch_size=self._batch_size,
            collate_fn=self._batch_padder.create_batch,
            num_workers=self._num_worker,
        )

from torch.utils.data.dataloader import DataLoader

from data import Vocab, GwtDataset, BatchPadder


BATCH_SIZE = 32

dataset_base_path = ' ../../datasets/bpe/results/data'
vocab = Vocab(f'{dataset_base_path}/bpe_ast_vocab.txt')
train_dataset = GwtDataset(f'{dataset_base_path}/bpe_ast_split/train.jsonl')
batch_padder = BatchPadder(vocab.get_index(vocab.PAD_TOKEN))
train_data_loader = DataLoader(train_dataset, batch_size=BATCH_SIZE, collate_fn=batch_padder.create_batch, shuffle=True)

import sys
import os
import json
import math

from torch.utils.data import random_split

import ast_sequence
from bpe import BpeProcessor
from data import Vocab


def create_ast_value_vocab(dataset_path, output_path, only_values, special_words=None):
    vocab = set()
    with open(dataset_path) as dataset_file:
        for json_line in dataset_file:
            datapoint = json.loads(json_line)
            vocab.update(
                token.replace('\n', BpeProcessor.NEW_LINE_TOKEN)
                for token in datapoint['src'] if not only_values or ast_sequence.Token.is_value(token)
            )
            vocab.update(
                token.replace('\n', BpeProcessor.NEW_LINE_TOKEN)
                for token in datapoint['trgTok'] if not only_values or ast_sequence.Token.is_value(token)
            )
    if special_words:
        for word in special_words:
            vocab.discard(word)
        save_vocab(output_path, special_words)
    save_vocab(output_path, vocab)


def save_vocab(path, vocab):
    with open(path, 'a+') as file:
        file.writelines(f'{word}\n' for word in vocab)


def bpe_encode_dataset(
    dataset_path,
    model_path,
    encoded_dataset_path,
    max_source_seq_length=None,
    max_target_seq_length=None,
    log_interval=1000,
):
    bpe_processor = BpeProcessor(model_path)
    visited = set()
    counter = 0
    with open(dataset_path) as dataset_file, open(encoded_dataset_path, 'w+') as output_dataset:
        for json_line in dataset_file:
            datapoint = json.loads(json_line)
            source_seq = bpe_processor.encode(datapoint['src'])
            target_seq = bpe_processor.encode(datapoint['trgTok'])
            if (
                (max_source_seq_length is None or len(source_seq) <= max_source_seq_length)
                and (max_target_seq_length is None or len(target_seq) <= max_target_seq_length)
            ):
                unique_data = (tuple(source_seq), tuple(target_seq))
                if unique_data in visited:
                    continue

                output_dataset.write(json.dumps({
                    'id': datapoint['id'],
                    'src': source_seq,
                    'trgTok': target_seq,
                    'trgCode': datapoint['trgCode'],
                    'testCtxCount': datapoint['testCtxCount'],
                    'ctxCount': datapoint['ctxCount'],
                }, separators=(',', ':')) + '\n')

                visited.add(unique_data)
                counter += 1

                if counter % log_interval == 0:
                    print(f'- Finished with item {counter}')


def create_encoded_dataset_split(data_split_dir_path, bpe_dataset_path, vocab_path, data_split):
    if not os.path.exists(data_split_dir_path):
        os.makedirs(data_split_dir_path)

    total_line_count = get_file_length(bpe_dataset_path)
    train_line_count = math.floor(data_split[0] * total_line_count)
    validation_line_count = math.floor(data_split[1] * total_line_count)
    test_line_count = math.floor(data_split[2] * total_line_count)
    train_line_count += total_line_count - (train_line_count + validation_line_count + test_line_count)

    train_lines, validation_lines, test_lines = [
        set(split) for split in random_split(
            range(total_line_count),
            (train_line_count, validation_line_count, test_line_count)
        )
    ]

    vocab = Vocab(vocab_path)

    sos_index = vocab.get_index(Vocab.SOS_TOKEN)
    eos_index = vocab.get_index(Vocab.EOS_TOKEN)

    with \
            open(bpe_dataset_path) as dataset_file, \
            open(f'{data_split_dir_path}/train.jsonl', 'w+') as train_data_file, \
            open(f'{data_split_dir_path}/validate.jsonl', 'w+') as validate_data_file, \
            open(f'{data_split_dir_path}/test.jsonl', 'w+') as test_data_file, \
            open(f'{data_split_dir_path}/train_ids.txt', 'w+') as train_data_ids_file, \
            open(f'{data_split_dir_path}/validate_ids.txt', 'w+') as validate_data_ids_file, \
            open(f'{data_split_dir_path}/test_ids.txt', 'w+') as test_data_ids_file:
        line_counter = 0
        for json_line in dataset_file:
            json_data = json.loads(json_line)
            data = json.dumps([
                [vocab.get_index(token) for token in json_data['src']],
                [sos_index] + [vocab.get_index(token) for token in json_data['trgTok']] + [eos_index],
            ], separators=(',', ':')) + '\n'

            if line_counter in train_lines:
                train_data_file.write(data)
                train_data_ids_file.write(json_data['id'] + '\n')
            elif line_counter in validation_lines:
                validate_data_file.write(data)
                validate_data_ids_file.write(json_data['id'] + '\n')
            else:
                test_data_file.write(data)
                test_data_ids_file.write(json_data['id'] + '\n')

            line_counter += 1


def encoded_predefined_dataset_split(data_split_dir_path, bpe_dataset_path, vocab_path):
    with \
            open(f'{data_split_dir_path}/train.ids.txt', 'w+') as train_data_file, \
            open(f'{data_split_dir_path}/validate.ids.txt', 'w+') as validate_data_file, \
            open(f'{data_split_dir_path}/test.ids.txt', 'w+') as test_data_file:
        train_ids = train_data_file.readlines()
        validate_ids = validate_data_file.readlines()
        test_ids = test_data_file.readlines()

    vocab = Vocab(vocab_path)
    sos_index = vocab.get_index(Vocab.SOS_TOKEN)
    eos_index = vocab.get_index(Vocab.EOS_TOKEN)

    with \
            open(bpe_dataset_path) as dataset_file, \
            open(f'{data_split_dir_path}/train.jsonl', 'w+') as train_data_file, \
            open(f'{data_split_dir_path}/validate.jsonl', 'w+') as validate_data_file, \
            open(f'{data_split_dir_path}/test.jsonl', 'w+') as test_data_file:
        for json_line in dataset_file:
            json_data = json.loads(json_line)
            data = json.dumps([
                [vocab.get_index(token) for token in json_data['src']],
                [sos_index] + [vocab.get_index(token) for token in json_data['trgTok']] + [eos_index],
            ], separators=(',', ':')) + '\n'

            if json_data['id'] in train_ids:
                train_data_file.write(data)
            elif json_data['id'] in validate_ids:
                validate_data_file.write(data)
            elif json_data['id'] in test_ids:
                test_data_file.write(data)
            else:
                raise ValueError(f'id {json_data["id"]} is not part of any of the splits')


def get_file_length(path):
    with open(path) as file:
        i = 0
        for i, _ in enumerate(file, 1):
            pass
    return i


if __name__ == '__main__':
    # TODO use UNK or unidecode for weird chars?
    # TODO unidecode swallows emojis (is UNK or not having a char better...?)
    VOCAB_SIZE = 16000

    input_dataset_path = sys.argv[1]
    output_dir = sys.argv[2]
    max_source_seq_length = int(sys.argv[3]) if len(sys.argv) > 3 else None
    max_target_seq_length = int(sys.argv[4]) if len(sys.argv) > 3 else None
    for directory in [output_dir, f'{output_dir}/data', f'{output_dir}/model']:
        if not os.path.exists(directory):
            os.makedirs(directory)

    model_path = f'{output_dir}/model'
    model_name = 'ast_values'
    raw_vocab_path = f'{output_dir}/data/raw_ast_value_vocab.txt'
    bpe_vocab_path = f'{output_dir}/data/bpe_ast_vocab.txt'
    output_dataset_path = f'{output_dir}/data/bpe_gwt.jsonl'
    data_split_dir_path = f'{output_dir}/data/bpe_ast_split'

    print('Start creating raw AST value vocab...')
    create_ast_value_vocab(input_dataset_path, raw_vocab_path, only_values=True)
    print('Start training BPE...')
    BpeProcessor.train(raw_vocab_path, model_name, model_path, VOCAB_SIZE)
    print('Start creating BPE encoded dataset...')
    bpe_encode_dataset(
        input_dataset_path,
        f'{model_path}/{model_name}.model',
        output_dataset_path,
        max_source_seq_length,
        max_target_seq_length
    )
    print('Start creating BPE encoded AST value vocab...')
    create_ast_value_vocab(
        output_dataset_path,
        bpe_vocab_path,
        only_values=False,
        special_words=[
            Vocab.PAD_TOKEN,
            Vocab.SOS_TOKEN,
            Vocab.EOS_TOKEN,
            BpeProcessor.NEW_LINE_TOKEN,
            BpeProcessor.UNKOWN_TOKEN,
        ],
    )
    print('Start creating data split')
    create_encoded_dataset_split(data_split_dir_path, output_dataset_path, bpe_vocab_path, (.8, .1, .1))

import sys
import os
import json
import math
from argparse import ArgumentParser

from torch.utils.data import random_split

from javalang import tokenizer

import ast_sequence
from bpe import BpeProcessor
from data import Vocab


class TargetFormat():
    AST = 'AST'
    CODE = 'CODE'


def create_ast_value_vocab(dataset_path, output_path, only_values, special_words=None):
    vocab = set()
    with open(dataset_path) as dataset_file:
        for datapoint in iterate_jsonl(dataset_file):
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


def remove_context_declarations_from_ast_sequence(
    sequence,
    context_opening_node,
    context_closing_node,
    test_context_opening_node,
    test_context_closing_node,
):
    final_sequence = []
    append = True

    for token in sequence:
        if token == context_opening_node or token == test_context_opening_node:
            append = False
        if append:
            final_sequence.append(token)
        if token == context_closing_node or token == test_context_closing_node:
            append = True

    return final_sequence


def bpe_encode_dataset(
    dataset_path,
    model_path,
    encoded_dataset_path,
    max_source_seq_length=None,
    max_target_seq_length=None,
    remove_context_declarations=False,
    log_interval=1000,
):
    bpe_processor = BpeProcessor(model_path)
    visited = set()
    counter = 0
    with open(dataset_path) as dataset_file, open(encoded_dataset_path, 'w+') as output_dataset:
        for datapoint in iterate_jsonl(dataset_file):
            source_seq = bpe_processor.encode(datapoint['src'])
            target_seq = bpe_processor.encode(datapoint['trgTok'])
            if (
                (max_source_seq_length is None or len(source_seq) <= max_source_seq_length)
                and (max_target_seq_length is None or len(target_seq) <= max_target_seq_length)
            ):
                unique_data = (tuple(source_seq), tuple(target_seq))
                if unique_data in visited:
                    continue

                if remove_context_declarations:
                    source_seq = remove_context_declarations_from_ast_sequence(source_seq)
                    target_seq = remove_context_declarations_from_ast_sequence(target_seq)

                output_dataset.write(dump_jsonl({
                    'id': datapoint['id'],
                    'src': source_seq,
                    'trgTok': target_seq,
                    'trgCode': datapoint['trgCode'],
                    'testCtxCount': datapoint['testCtxCount'] if not remove_context_declarations else 0,
                    'ctxCount': datapoint['ctxCount'] if not remove_context_declarations else 0,
                }))

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
            open(f'{data_split_dir_path}/test_ids.txt', 'w+') as test_data_ids_file, \
            open(f'{data_split_dir_path}/validate_code_tokens.jsonl', 'w+') as validate_code_tokens_file, \
            open(f'{data_split_dir_path}/test_code_tokens.jsonl', 'w+') as test_code_tokens_file:
        line_counter = 0
        for json_data in iterate_jsonl(dataset_file):
            src_data = [vocab.get_index(token) for token in json_data['src']]
            data = dump_jsonl([
                src_data,
                [sos_index] + [vocab.get_index(token) for token in json_data['trgTok']] + [eos_index],
            ])

            if line_counter in train_lines:
                train_data_file.write(data)
                train_data_ids_file.write(json_data['id'] + '\n')
            else:
                code_tokens = dump_jsonl(
                    [src_data, [token.value for token in tokenizer.tokenize(json_data['trgCode'])]],
                )
                if line_counter in validation_lines:
                    validate_data_file.write(data)
                    validate_data_ids_file.write(json_data['id'] + '\n')
                    validate_code_tokens_file.write(code_tokens)
                elif line_counter in test_lines:
                    test_data_file.write(data)
                    test_data_ids_file.write(json_data['id'] + '\n')
                    test_code_tokens_file.write(code_tokens)
                else:
                    raise ValueError(f'id {json_data["id"]} is not part of any of the splits')

            line_counter += 1


def encoded_predefined_dataset_split(
    data_split_dir_path,
    bpe_dataset_path,
    vocab_path,
    remove_context_declarations=False,
):
    with \
            open(f'{data_split_dir_path}/train_ids.txt') as train_data_file, \
            open(f'{data_split_dir_path}/validate_ids.txt') as validate_data_file, \
            open(f'{data_split_dir_path}/test_ids.txt') as test_data_file:
        train_ids = [line[:-1] for line in train_data_file.readlines()]
        validate_ids = [line[:-1] for line in validate_data_file.readlines()]
        test_ids = [line[:-1] for line in test_data_file.readlines()]

    vocab = Vocab(vocab_path)
    sos_index = vocab.get_index(Vocab.SOS_TOKEN)
    eos_index = vocab.get_index(Vocab.EOS_TOKEN)
    if remove_context_declarations:
        ctx_open_id = vocab.get_index(ast_sequence.Token.OPEN_CONTEXT)
        ctx_close_id = vocab.get_index(ast_sequence.Token.CLOSE_CONTEXT)
        test_ctx_open_id = vocab.get_index(ast_sequence.Token.OPEN_TEST_CONTEXT)
        test_ctx_close_id = vocab.get_index(ast_sequence.Token.CLOSE_TEST_CONTEXT)

    with \
            open(bpe_dataset_path) as dataset_file, \
            open(f'{data_split_dir_path}/train.jsonl', 'w+') as train_data_file, \
            open(f'{data_split_dir_path}/validate.jsonl', 'w+') as validate_data_file, \
            open(f'{data_split_dir_path}/test.jsonl', 'w+') as test_data_file, \
            open(f'{data_split_dir_path}/validate_code_tokens.jsonl', 'w+') as validate_code_tokens_file, \
            open(f'{data_split_dir_path}/test_code_tokens.jsonl', 'w+') as test_code_tokens_file:
        for json_data in iterate_jsonl(dataset_file):
            src_data = [vocab.get_index(token) for token in json_data['src']]
            trg_data = [vocab.get_index(token) for token in json_data['trgTok']]

            if remove_context_declarations:
                src_data = remove_context_declarations_from_ast_sequence(
                    src_data,
                    ctx_open_id,
                    ctx_close_id,
                    test_ctx_open_id,
                    test_ctx_close_id,
                )
                trg_data = remove_context_declarations_from_ast_sequence(
                    trg_data,
                    ctx_open_id,
                    ctx_close_id,
                    test_ctx_open_id,
                    test_ctx_close_id,
                )

            data = dump_jsonl([
                src_data,
                [sos_index] + trg_data + [eos_index],
            ])

            if json_data['id'] in train_ids:
                train_data_file.write(data)
            else:
                code_tokens = dump_jsonl(
                    [src_data, [token.value for token in tokenizer.tokenize(json_data['trgCode'])]]
                )
                if json_data['id'] in validate_ids:
                    validate_data_file.write(data)
                    validate_code_tokens_file.write(code_tokens)
                elif json_data['id'] in test_ids:
                    test_data_file.write(data)
                    test_code_tokens_file.write(code_tokens)
                else:
                    raise ValueError(f'id {json_data["id"]} is not part of any of the splits')


def tokenize_target_data(input_dataset_path, output_path):
    with open(input_dataset_path) as input_dataset, open(output_path, 'w+') as tokenized_dataset:
        # TODO
        pass


def get_file_length(path):
    with open(path) as file:
        i = 0
        for i, _ in enumerate(file, 1):
            pass
    return i


def iterate_jsonl(file):
    for json_line in file:
        yield json.loads(json_line)


def dump_jsonl(data):
    return json.dumps(data, separators=(',', ':')) + '\n'


if __name__ == '__main__':
    parser = ArgumentParser()
    parser.add_argument('--bpe_vocab_size', type=int, default=16000)
    parser.add_argument('--input_dataset_path', type=str, required=True)
    parser.add_argument('--output_dir', type=str, required=True)
    parser.add_argument('--tokenize_input_dataset_target_code', type=bool, default=False)
    parser.add_argument(
        '--target_format',
        type=str,
        default=TargetFormat.AST,
        choices=(TargetFormat.AST, TargetFormat.CODE,)
    )
    parser.add_argument('--max_source_seq_length', type=int, default=None)
    parser.add_argument('--max_target_seq_length', type=int, default=None)
    args = parser.parse_args()

    for directory in [args.output_dir, f'{args.output_dir}/data', f'{args.output_dir}/model']:
        if not os.path.exists(directory):
            os.makedirs(directory)

    model_path = f'{args.output_dir}/model'
    model_name = 'ast_values'
    raw_vocab_path = f'{args.output_dir}/data/raw_ast_value_vocab.txt'
    bpe_vocab_path = f'{args.output_dir}/data/bpe_ast_vocab.txt'
    output_dataset_path = f'{args.output_dir}/data/bpe_gwt.jsonl'
    data_split_dir_path = f'{args.output_dir}/data/bpe_ast_split'

    if args.tokenize_input_dataset_target_code:
        print('Start tokenizing target data...')
        tokenized_dataset_path = f'{args.input_dataset_path.split(".jsonl")[0]}_tokenized.jsonl'
        tokenize_target_data(args.input_dataset_path, tokenized_dataset_path)
        args.input_dataset_path = tokenized_dataset_path
    print('Start creating raw AST value vocab...')
    create_ast_value_vocab(args.input_dataset_path, raw_vocab_path, only_values=True)
    print('Start training BPE...')
    BpeProcessor.train(raw_vocab_path, model_name, model_path, args.bpe_vocab_size)
    print('Start creating BPE encoded dataset...')
    bpe_encode_dataset(
        args.input_dataset_path,
        f'{model_path}/{model_name}.model',
        output_dataset_path,
        args.max_source_seq_length,
        args.max_target_seq_length,
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

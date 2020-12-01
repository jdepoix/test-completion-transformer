import os
import json
import math
from argparse import ArgumentParser
import random

from torch.utils.data import random_split

import ast_sequence
import source_code
from source_code import TestDeclaration
from bpe import BpeProcessor
from data import Vocab


class TargetFormat():
    AST = 'AST'
    CODE = 'CODE'

    @staticmethod
    def get_source_key(format):
        if format == TargetFormat.AST:
            return 'src'
        if format == TargetFormat.CODE:
            return 'srcCodeTok'
        raise ValueError('invalid format')

    @staticmethod
    def get_target_key(format):
        if format == TargetFormat.AST:
            return 'trgTok'
        if format == TargetFormat.CODE:
            return 'trgCodeTok'
        raise ValueError('invalid format')


def create_ast_value_vocab(dataset_path, output_path, only_values, special_words=None, target_format=TargetFormat.AST):
    vocab = set()
    with open(dataset_path) as dataset_file:
        for datapoint in iterate_jsonl(dataset_file):
            source_data = datapoint[TargetFormat.get_source_key(target_format)]
            target_data = datapoint[TargetFormat.get_target_key(target_format)]

            if source_data is not None:
                vocab.update(
                    token.replace('\n', BpeProcessor.NEW_LINE_TOKEN)
                    for token in source_data if not only_values or ast_sequence.Token.is_value(token)
                )
            if target_data is not None:
                vocab.update(
                    token.replace('\n', BpeProcessor.NEW_LINE_TOKEN)
                    for token in target_data if not only_values or ast_sequence.Token.is_value(token)
                )
    if special_words:
        for word in special_words:
            vocab.discard(word)
        save_vocab(output_path, special_words)
    save_vocab(output_path, vocab)


def save_vocab(path, vocab):
    with open(path, 'a+') as file:
        for word in vocab:
            try:
                file.write(f'{word}\n')
            except UnicodeEncodeError:
                print(f'- Could not encode the following word to vocab: {word.encode("utf-8", "replace").decode()}')


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
    target_format=TargetFormat.AST,
    ast_model_path=None,
):
    bpe_processor = BpeProcessor(model_path)
    if target_format == TargetFormat.CODE:
        if ast_model_path is None:
            raise ValueError('AST BPE model path needs to be provided when target format is CODE!')
        ast_bpe_processor = BpeProcessor(ast_model_path)
    visited = set()
    counter = 0
    duplicate_counter = 0
    with open(dataset_path) as dataset_file, open(encoded_dataset_path, 'w+') as output_dataset:
        for datapoint in iterate_jsonl(dataset_file):
            source_data = datapoint[TargetFormat.get_source_key(target_format)]
            target_data = datapoint[TargetFormat.get_target_key(target_format)]
            if source_data is None or target_data is None:
                continue

            try:
                source_seq = bpe_processor.encode(source_data)
                target_seq = bpe_processor.encode(target_data)
            except TypeError:
                print(f'- skipped {datapoint["id"]} as it is not encodable!')
                continue

            if target_format == TargetFormat.AST:
                source_seq_length = len(source_seq)
                target_seq_length = len(target_seq)
            else:
                source_seq_length = len(
                    ast_bpe_processor.encode(datapoint[TargetFormat.get_source_key(TargetFormat.AST)])
                )
                target_seq_length = len(
                    ast_bpe_processor.encode(datapoint[TargetFormat.get_target_key(TargetFormat.AST)])
                )
            if (
                (max_source_seq_length is None or source_seq_length <= max_source_seq_length)
                and (max_target_seq_length is None or target_seq_length <= max_target_seq_length)
            ):
                unique_data = (tuple(source_seq), tuple(target_seq))
                if unique_data in visited:
                    duplicate_counter += 1
                    continue

                if remove_context_declarations:
                    source_seq = remove_context_declarations_from_ast_sequence(source_seq)

                output_dataset.write(dump_jsonl({
                    'id': datapoint['id'],
                    'src': source_seq if target_format == TargetFormat.AST else datapoint['src'],
                    'srcCodeTok': source_seq if target_format == TargetFormat.CODE else datapoint['srcCodeTok'],
                    'trgTok': target_seq if target_format == TargetFormat.AST else datapoint['trgTok'],
                    'trgCodeTok': target_seq if target_format == TargetFormat.CODE else datapoint['trgCodeTok'],
                    'trgCode': datapoint['trgCodeTok'],
                    'testCtxCount': datapoint['testCtxCount'] if not remove_context_declarations else 0,
                    'ctxCount': datapoint['ctxCount'] if not remove_context_declarations else 0,
                }))

                visited.add(unique_data)
                counter += 1

                if counter % log_interval == 0:
                    print(f'- Finished with item {counter}')
    print(f'-> Filtered duplicates: {duplicate_counter}')


def create_encoded_dataset_split(
    data_split_dir_path,
    bpe_dataset_path,
    vocab_path,
    data_split,
    target_format=TargetFormat.AST,
):
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
            source_data = json_data[TargetFormat.get_source_key(target_format)]
            target_data = json_data[TargetFormat.get_target_key(target_format)]
            if source_data is None or target_data is None:
                continue

            src_data = [vocab.get_index(token) for token in source_data]
            data = dump_jsonl([
                src_data,
                [sos_index]
                + [vocab.get_index(token) for token in target_data]
                + [eos_index],
            ])

            if line_counter in train_lines:
                train_data_file.write(data)
                train_data_ids_file.write(json_data['id'] + '\n')
            else:
                code_tokens = dump_jsonl([src_data, json_data['trgCode']]) \
                    if json_data['trgCode'] is not None else None
                if line_counter in validation_lines:
                    validate_data_file.write(data)
                    validate_data_ids_file.write(json_data['id'] + '\n')
                    if code_tokens:
                        validate_code_tokens_file.write(code_tokens)
                elif line_counter in test_lines:
                    test_data_file.write(data)
                    test_data_ids_file.write(json_data['id'] + '\n')
                    if code_tokens:
                        test_code_tokens_file.write(code_tokens)
                else:
                    raise ValueError(f'id {json_data["id"]} is not part of any of the splits')

            line_counter += 1


def encode_predefined_dataset_split(
    data_split_dir_path,
    bpe_dataset_path,
    vocab_path,
    remove_context_declarations=False,
    target_format=TargetFormat.AST,
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
        ctx_open_id = vocab.get_index(ast_sequence.Token.CONTEXT_OPEN)
        ctx_close_id = vocab.get_index(ast_sequence.Token.CONTEXT_CLOSE)
        test_ctx_open_id = vocab.get_index(ast_sequence.Token.TEST_CONTEXT_OPEN)
        test_ctx_close_id = vocab.get_index(ast_sequence.Token.TEST_CONTEXT_CLOSE)

    with \
            open(bpe_dataset_path) as dataset_file, \
            open(f'{data_split_dir_path}/train.jsonl', 'w+') as train_data_file, \
            open(f'{data_split_dir_path}/validate.jsonl', 'w+') as validate_data_file, \
            open(f'{data_split_dir_path}/test.jsonl', 'w+') as test_data_file, \
            open(f'{data_split_dir_path}/validate_code_tokens.jsonl', 'w+') as validate_code_tokens_file, \
            open(f'{data_split_dir_path}/test_code_tokens.jsonl', 'w+') as test_code_tokens_file:
        for json_data in iterate_jsonl(dataset_file):
            source_data = json_data[TargetFormat.get_source_key(target_format)]
            target_data = json_data[TargetFormat.get_target_key(target_format)]
            if source_data is None or target_data is None:
                continue

            src_data = [vocab.get_index(token) for token in source_data]
            trg_data = [vocab.get_index(token) for token in target_data]

            if remove_context_declarations:
                if target_format == TargetFormat.CODE:
                    raise NotImplementedError(
                        'removing context declarations is not supported for target format CODE yet'
                    )
                src_data = remove_context_declarations_from_ast_sequence(
                    src_data,
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
                code_tokens = dump_jsonl([src_data, json_data['trgCode']]) \
                    if json_data['trgCode'] is not None else None
                if json_data['id'] in validate_ids:
                    validate_data_file.write(data)
                    if code_tokens:
                        validate_code_tokens_file.write(code_tokens)
                elif json_data['id'] in test_ids:
                    test_data_file.write(data)
                    if code_tokens:
                        test_code_tokens_file.write(code_tokens)
                else:
                    print(f'- id {json_data["id"]} is not part of any of the splits')


def tokenize_target_data(input_dataset_path, output_path):
    with open(input_dataset_path) as input_dataset, open(output_path, 'w+') as tokenized_dataset:
        for datapoint in iterate_jsonl(input_dataset):
            tokenized_dataset.write(dump_jsonl({
                'id': datapoint['id'],
                'src': datapoint['src'],
                'srcCodeTok': TestDeclaration.tokenize(datapoint['testDec']),
                'trgTok': datapoint['trgTok'],
                'trgCodeTok': tokenize_code_safe(datapoint['trgCode']),
                'testCtxCount': datapoint['testCtxCount'],
                'ctxCount': datapoint['ctxCount'],
            }))


def sample_file(input_path, output_path, sample_size):
    with open(input_path) as input_file, open(output_path, 'w+') as output_file:
        output_file.writelines(random.sample(input_file.readlines(), sample_size))


def get_file_length(path):
    with open(path) as file:
        i = 0
        for i, _ in enumerate(file, 1):
            pass
    return i


def iterate_jsonl(file):
    for json_line in file:
        yield json.loads(json_line)


def tokenize_code_safe(code):
    try:
        return source_code.tokenize(code)
    except:
        return None


def dump_jsonl(data):
    return json.dumps(data, separators=(',', ':')) + '\n'


def get_argpaser():
    parser = ArgumentParser()
    parser.add_argument('--bpe_vocab_size', type=int, default=16000)
    parser.add_argument('--input_dataset_path', type=str, required=True)
    parser.add_argument('--output_dir', type=str, required=True)
    parser.add_argument('--data_split_dir_path', type=str, default=None)
    parser.add_argument('--tokenize_input_dataset', type=bool, default=False)
    parser.add_argument(
        '--target_format',
        type=str,
        default=TargetFormat.AST,
        choices=(TargetFormat.AST, TargetFormat.CODE,)
    )
    parser.add_argument('--max_source_seq_length', type=int, default=None)
    parser.add_argument('--max_target_seq_length', type=int, default=None)
    parser.add_argument('--ast_model_path', type=str, default=None)
    parser.add_argument('--skip_to_step', type=int, default=None)
    return parser


if __name__ == '__main__':
    args = get_argpaser().parse_args()

    for directory in [args.output_dir, f'{args.output_dir}/data', f'{args.output_dir}/model']:
        if not os.path.exists(directory):
            os.makedirs(directory)

    model_path = f'{args.output_dir}/model'
    model_name = 'ast_values'
    raw_vocab_path = f'{args.output_dir}/data/raw_ast_value_vocab.txt'
    bpe_vocab_path = f'{args.output_dir}/data/bpe_ast_vocab.txt'
    output_dataset_path = f'{args.output_dir}/data/bpe_gwt.jsonl'
    data_split_dir_path = args.data_split_dir_path \
        if args.data_split_dir_path else f'{args.output_dir}/data/bpe_ast_split'

    if args.tokenize_input_dataset:
        print('Start tokenizing dataset...')
        tokenized_dataset_path = f'{args.input_dataset_path.split(".jsonl")[0]}_tokenized.jsonl'
        tokenize_target_data(args.input_dataset_path, tokenized_dataset_path)
        args.input_dataset_path = tokenized_dataset_path

    if args.skip_to_step is None or args.skip_to_step <= 1:
        print('Start creating raw AST value vocab...')
        create_ast_value_vocab(
            args.input_dataset_path,
            raw_vocab_path,
            only_values=True,
            target_format=args.target_format
        )
    if args.skip_to_step is None or args.skip_to_step <= 2:
        print('Start training BPE...')
        BpeProcessor.train(raw_vocab_path, model_name, model_path, args.bpe_vocab_size)
    if args.skip_to_step is None or args.skip_to_step <= 3:
        print('Start creating BPE encoded dataset...')
        bpe_encode_dataset(
            args.input_dataset_path,
            f'{model_path}/{model_name}.model',
            output_dataset_path,
            args.max_source_seq_length,
            args.max_target_seq_length,
            target_format=args.target_format,
            ast_model_path=args.ast_model_path,
        )
    if args.skip_to_step is None or args.skip_to_step <= 4:
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
            target_format=args.target_format,
        )
    if args.skip_to_step is None or args.skip_to_step <= 5:
        print('Start creating/encoding data split')
        if args.data_split_dir_path is None:
            create_encoded_dataset_split(
                data_split_dir_path,
                output_dataset_path,
                bpe_vocab_path,
                (.8, .1, .1),
                target_format=args.target_format,
            )
        else:
            encode_predefined_dataset_split(
                data_split_dir_path,
                output_dataset_path,
                bpe_vocab_path,
                target_format=args.target_format,
            )

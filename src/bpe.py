import shutil

import sentencepiece

import ast_sequence


class BpeProcessor():
    NEW_LINE_TOKEN = '<_N_>'

    def __init__(self, model_path):
        self._model = sentencepiece.SentencePieceProcessor(model_file=model_path)

    def encode(self, tokens):
        encoded_tokens = []
        for token in tokens:
            if ast_sequence.Token.is_value(token):
                encoded_tokens += self._model.encode(token.replace('\n', self.NEW_LINE_TOKEN), out_type=str)
            else:
                encoded_tokens.append(token)
        return encoded_tokens

    def decode(self, tokens):
        decoded_tokens = []
        for token in tokens:
            if ast_sequence.Token.is_value(token):
                decoded_token = self._model.decode(token, out_type=str)
                decoded_tokens.append(decoded_token if decoded_token != self.NEW_LINE_TOKEN else '\n')
            else:
                decoded_tokens.append(token)
        return decoded_tokens

    @staticmethod
    def train(raw_vocab_path, model_name, model_path, vocab_size):
        sentencepiece.SentencePieceTrainer.train(
            input=raw_vocab_path,
            model_prefix=model_name,
            model_type='bpe',
            vocab_size=vocab_size,
            user_defined_symbols=list(range(10)) + [BpeProcessor.NEW_LINE_TOKEN]
        )
        shutil.move(f'{model_name}.model', f'{model_path}/{model_name}.model')
        shutil.move(f'{model_name}.vocab', f'{model_path}/{model_name}.vocab')

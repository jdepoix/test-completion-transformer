import itertools
from abc import ABC, abstractmethod

from javalang import tokenizer


def tokenize(code):
    return [token.value for token in tokenizer.tokenize(code)]


class SourceCodeProcessor(ABC):
    def __init__(self, sequentialization_client):
        self._sequentialization_client = sequentialization_client

    @abstractmethod
    def encode(
        self,
        test_file_content,
        test_class_name,
        test_method_signature,
        related_file_content,
        related_class_name,
        related_method_signature,
        then_section_start_index=None,
    ):
        pass

    @abstractmethod
    def decode(self, encoded_sequence):
        pass


class AstSequenceProcessor(SourceCodeProcessor):
    def encode(
        self,
        test_file_content,
        test_class_name,
        test_method_signature,
        related_file_content,
        related_class_name,
        related_method_signature,
        then_section_start_index=None,
    ):
        return self._sequentialization_client.create_test_declaration_ast_sequence(
            test_file_content,
            test_class_name,
            test_method_signature,
            related_file_content,
            related_class_name,
            related_method_signature,
            then_section_start_index,
        )

    def decode(self, encoded_sequence):
        return self._sequentialization_client.parse_then_sequence_to_code(encoded_sequence)


class TokenizedCodeProcessor(SourceCodeProcessor):
    def encode(
        self,
        test_file_content,
        test_class_name,
        test_method_signature,
        related_file_content,
        related_class_name,
        related_method_signature,
        then_section_start_index=None,
    ):
        test_declaration = self._sequentialization_client.create_test_declaration(
            test_file_content,
            test_class_name,
            test_method_signature,
            related_file_content,
            related_class_name,
            related_method_signature,
            then_section_start_index,
        )
        return TestDeclaration.tokenize(test_declaration)

    def decode(self, encoded_sequence):
        return self._sequentialization_client.check_parsability(' '.join(encoded_sequence))


class TestDeclaration():
    TEST_NAME_MARK = '<[TEST_NAME]>'
    TEST_BODY_MARK = '<[TEST_BODY]>'
    TEST_CONTEXT_DECLARATION_MARK = '<[TEST_CONTEXT_DECLARATION]>'
    WHEN_DECLARATION_MARK = '<[WHEN_DECLARATION]>'
    CONTEXT_DECLARATION_MARK = '<[CONTEXT_DECLARATION]>'

    @staticmethod
    def tokenize(test_declaration):
        try:
            return (
                [
                    TestDeclaration.TEST_NAME_MARK, test_declaration['name'],
                    TestDeclaration.TEST_BODY_MARK,
                ] + tokenize(test_declaration['body'])
                + TestDeclaration._tokenize_method_declarations(
                    test_declaration['testCtx'],
                    TestDeclaration.TEST_CONTEXT_DECLARATION_MARK
                )
                + [TestDeclaration.WHEN_DECLARATION_MARK] + tokenize(test_declaration['when'])
                + TestDeclaration._tokenize_method_declarations(
                    test_declaration['ctx'],
                    TestDeclaration.CONTEXT_DECLARATION_MARK
                )
            )
        except Exception:
            return None

    @staticmethod
    def _tokenize_method_declarations(method_declarations, mark):
        return list(
            itertools.chain.from_iterable(
                [[mark] + tokenize(method_declaration) for method_declaration in method_declarations]
            )
        )

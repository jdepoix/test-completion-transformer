from socket import socket, AF_INET, SOCK_STREAM
import json


class AstSequentializationApiClient():
    """
    Client for the Java Sequentialization API used to create the Test Declaration AST and generate source code for
    AST sequences.
    """

    class ApiError(Exception):
        pass

    class _Status():
        ERROR = 'ERROR'
        SUCESS = 'SUCCESSFUL'

    class _Command():
        CREATE_TEST_DECLARATION = 'CREATE_TEST_DECLARATION'
        CREATE_TEST_DECLARATION_AST_SEQUENCE = 'CREATE_TEST_DECLARATION_AST_SEQUENCE'
        THEN_SEQUENCE_TO_CODE = 'THEN_SEQUENCE_TO_CODE'
        CHECK_PARSABILITY = 'CHECK_PARSABILITY'

    def __init__(self, address, port):
        self._address = address
        self._port = port

    def create_test_declaration(
        self,
        test_file_content,
        test_class_name,
        test_method_signature,
        related_file_content,
        related_class_name,
        related_method_signature,
        then_section_start_index=None,
    ):
        return self._send_message({
            'command': self._Command.CREATE_TEST_DECLARATION,
            'data': {
                'testFileContent': test_file_content,
                'testClassName': test_class_name,
                'testMethodSignature': test_method_signature,
                'relatedFileContent': related_file_content,
                'relatedClassName': related_class_name,
                'relatedMethodSignature': related_method_signature,
                'thenSectionStartIndex': then_section_start_index,
            }
        })

    def create_test_declaration_ast_sequence(
        self,
        test_file_content,
        test_class_name,
        test_method_signature,
        related_file_content,
        related_class_name,
        related_method_signature,
        then_section_start_index=None,
    ):
        return self._send_message({
            'command': self._Command.CREATE_TEST_DECLARATION_AST_SEQUENCE,
            'data': {
                'testFileContent': test_file_content,
                'testClassName': test_class_name,
                'testMethodSignature': test_method_signature,
                'relatedFileContent': related_file_content,
                'relatedClassName': related_class_name,
                'relatedMethodSignature': related_method_signature,
                'thenSectionStartIndex': then_section_start_index,
            }
        })

    def parse_then_sequence_to_code(self, sequence):
        return self._send_message({
            'command': self._Command.THEN_SEQUENCE_TO_CODE,
            'data': sequence
        })

    def check_parsability(self, code):
        return self._send_message({
            'command': self._Command.CHECK_PARSABILITY,
            'data': code
        })

    def _open_socket(self):
        s = socket(AF_INET, SOCK_STREAM)
        s.connect((self._address, self._port))
        return s

    def _send_message(self, message):
        s = self._open_socket()
        try:
            s.send(f'{json.dumps(message)}\n'.encode())
            response = json.loads(self._receive_all(s, 4096))
            if (
                response.get('status', self._Status.ERROR) != self._Status.SUCESS
                or 'data' not in response
            ):
                raise AstSequentializationApiClient.ApiError()
            return response['data']
        finally:
            s.close()

    def _receive_all(self, sock, buffer_size):
        response = b''
        while True:
            chunk = sock.recv(buffer_size)
            response += chunk
            if len(chunk) < buffer_size:
                return response.decode()

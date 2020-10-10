from socket import socket, AF_INET, SOCK_STREAM
import json


class AstSequentializationApiClient():
    class ApiError(Exception):
        pass

    class _Status():
        ERROR = 'ERROR'
        SUCESS = 'SUCCESSFUL'

    class _Command():
        CREATE_TEST_DECLARATION_SEQUENCE = 'CREATE_TEST_DECLARATION_SEQUENCE'
        THEN_SEQUENCE_TO_CODE = 'THEN_SEQUENCE_TO_CODE'

    def __init__(self, address, port):
        self._address = address
        self._port = port

    def create_test_declaration_sequence(
        self,
        test_file_content,
        test_class_name,
        test_method_signature,
        related_file_content,
        related_class_name,
        related_method_signature,
    ):
        return self._send_message({
            'command': self._Command.CREATE_TEST_DECLARATION_SEQUENCE,
            'data': {
                'testFileContent': test_file_content,
                'testClassName': test_class_name,
                'testMethodSignature': test_method_signature,
                'relatedFileContent': related_file_content,
                'relatedClassName': related_class_name,
                'relatedMethodSignature': related_method_signature,
            }
        })

    def parse_then_sequence_to_code(self, sequence):
        return self._send_message({
            'command': self._Command.THEN_SEQUENCE_TO_CODE,
            'data': sequence
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

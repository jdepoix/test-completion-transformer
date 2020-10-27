package org.jdepoix.dataset.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;

class RequestMessage {
    enum Command {
        CREATE_TEST_DECLARATION_SEQUENCE, THEN_SEQUENCE_TO_CODE
    }

    public Command command;
    public JsonNode data;
}

class ResponseMessage {
    enum Status {
        SUCCESSFUL, ERROR
    }

    public Status status;
    public JsonNode data;

    public ResponseMessage(Status status) {
        this.status = status;
        this.data = null;
    }

    public ResponseMessage(Status status, JsonNode data) {
        this.status = status;
        this.data = data;
    }
}

class MessageHandler implements Runnable {
    private static final ResponseMessage ERROR_RESPONSE = new ResponseMessage(ResponseMessage.Status.ERROR);

    private final Socket socket;

    public MessageHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final RequestMessage requestMessage = new ObjectMapper().readValue(reader.readLine(), RequestMessage.class);
            switch (requestMessage.command) {
                case THEN_SEQUENCE_TO_CODE:
                    sendResponse(new ResponseMessage(
                        ResponseMessage.Status.SUCCESSFUL,
                        new ThenSequenceToCodeCommand().execute(requestMessage.data)
                    ));
                    break;
                case CREATE_TEST_DECLARATION_SEQUENCE:
                    sendResponse(new ResponseMessage(
                        ResponseMessage.Status.SUCCESSFUL,
                        new CreateTestDeclarationSequenceCommand().execute(requestMessage.data)
                    ));
                    break;
                default:
                    sendResponse(ERROR_RESPONSE);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (!this.socket.isClosed()) {
                    sendResponse(ERROR_RESPONSE);
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {}
            }

            if (!this.socket.isClosed()) {
                try {
                    this.socket.close();
                } catch (IOException e) {}
            }
        }
    }

    private void sendResponse(ResponseMessage message) throws IOException {
        try(final OutputStreamWriter writer = new OutputStreamWriter(this.socket.getOutputStream())) {
            new ObjectMapper().writeValue(writer, message);
        }
    }
}

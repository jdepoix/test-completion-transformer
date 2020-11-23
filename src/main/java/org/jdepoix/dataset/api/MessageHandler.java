package org.jdepoix.dataset.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;
import java.util.Optional;

class RequestMessage {
    enum Command {
        CREATE_TEST_DECLARATION,
        CREATE_TEST_DECLARATION_AST_SEQUENCE,
        THEN_SEQUENCE_TO_CODE,
        CHECK_PARSABILITY
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

            Optional<Command> command = initCommand(requestMessage);
            if (command.isPresent()) {
                sendResponse(new ResponseMessage(
                    ResponseMessage.Status.SUCCESSFUL,
                    command.get().execute(requestMessage.data)
                ));
            } else {
                sendResponse(ERROR_RESPONSE);
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

    private Optional<Command> initCommand(RequestMessage requestMessage) {
        switch (requestMessage.command) {
            case CREATE_TEST_DECLARATION:
                return Optional.of(new CreateTestDeclarationCommand());
            case CREATE_TEST_DECLARATION_AST_SEQUENCE:
                return Optional.of(new CreateTestDeclarationAstSequenceCommand());
            case THEN_SEQUENCE_TO_CODE:
                return Optional.of(new ThenSequenceToCodeCommand());
            case CHECK_PARSABILITY:
                return Optional.of(new CheckParsabilityCommand());
            default:
                return Optional.empty();
        }
    }

    private void sendResponse(ResponseMessage message) throws IOException {
        try(final OutputStreamWriter writer = new OutputStreamWriter(this.socket.getOutputStream())) {
            new ObjectMapper().writeValue(writer, message);
        }
    }
}

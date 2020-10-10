package org.jdepoix.dataset.api;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

class AstSerializationServer {
    void run(int port) throws IOException {
        final ServerSocket serverSocket = new ServerSocket(port);

        while (true) {
            final Socket socket = serverSocket.accept();
            new Thread(new MessageHandler(socket)).run();
        }
    }
}

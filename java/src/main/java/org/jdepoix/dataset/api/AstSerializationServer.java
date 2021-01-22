package org.jdepoix.dataset.api;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class AstSerializationServer {
    private final int port;
    private final int workerCount;

    public AstSerializationServer(int port, int workerCount) {
        this.port = port;
        this.workerCount = workerCount;
    }

    void run() throws IOException {
        final ExecutorService executor = Executors.newFixedThreadPool(this.workerCount);
        final ServerSocket serverSocket = new ServerSocket(this.port);

        try {
            while (true) {
                final Socket socket = serverSocket.accept();
                executor.execute(new MessageHandler(socket));
            }
        } finally {
            executor.shutdown();
            serverSocket.close();
        }
    }
}

package org.jdepoix.dataset.api;

import java.io.IOException;

public class App {
    public static void main(String[] args) throws IOException {
        new AstSerializationServer(Integer.parseInt(args[0]), Integer.parseInt(args[1])).run();
    }
}

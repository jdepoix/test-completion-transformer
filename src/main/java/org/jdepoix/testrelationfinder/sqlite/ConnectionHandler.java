package org.jdepoix.testrelationfinder.sqlite;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionHandler implements AutoCloseable {
    private final Path dbFile;
    private Connection connection;
    private FileChannel fileChannel;
    private FileLock lock;

    public ConnectionHandler(Path dbFile) {
        this.dbFile = dbFile;
        this.connection = null;
    }

    public Connection getConnection() throws SQLException, IOException {
        this.acquireLock();
        try {
            this.connection = DriverManager.getConnection(String.format("jdbc:sqlite:%s", this.dbFile.toString()));
        } finally {
            this.releaseLock();
        }
        return this.connection;
    }

    @Override
    public void close() throws Exception {
        try {
            this.connection.close();
        } finally {
            this.releaseLock();
        }
    }

    private void acquireLock() throws IOException {
        this.releaseLock();
        this.fileChannel = new RandomAccessFile(this.dbFile.toFile(), "rw").getChannel();
        this.lock = fileChannel.lock();
    }

    private void releaseLock() throws IOException {
        if (this.lock != null && this.lock.isValid()) {
            this.lock.release();
            this.lock = null;
        }
        if (this.fileChannel != null && this.fileChannel.isOpen()) {
            this.fileChannel.close();
            this.fileChannel = null;
        }
    }
}

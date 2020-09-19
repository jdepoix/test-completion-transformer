package org.jdepoix.sqlite;

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
        if (this.connection != null) {
            return this.connection;
        }

        try {
            this.acquireLock();
            this.connection = DriverManager.getConnection(String.format("jdbc:sqlite:%s", this.dbFile.toString()));
        } catch (Exception exception) {
            this.closeConnectionAndRelease();
            throw exception;
        }
        return this.connection;
    }

    @Override
    public void close() throws Exception {
        this.closeConnectionAndRelease();
    }

    private void closeConnectionAndRelease() throws SQLException, IOException {
        try {
            if (this.connection != null && !this.connection.isClosed()) {
                this.connection.close();
            }
        } finally {
            this.releaseLock();
            this.connection = null;
        }
    }

    private void acquireLock() throws IOException {
        this.fileChannel = new RandomAccessFile(
            this.dbFile.toAbsolutePath().toString() + ".lock",
            "rw"
        ).getChannel();
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

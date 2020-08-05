package org.jdepoix.testrelationfinder.concurrency;

import java.util.LinkedList;
import java.util.Queue;

public class Channel<T> {
    private final Queue<T> messages = new LinkedList<>();
    private boolean closed = false;

    public synchronized void send(T message) {
        this.messages.add(message);
        this.notify();
    }

    public synchronized T receive() throws IsFinished {
        try {
            while (this.messages.isEmpty() && !this.closed) {
                this.wait();
            }

            if (isFinished()) {
                throw new IsFinished();
            }

            return this.messages.poll();
        } catch (InterruptedException e) {
            this.close();
            throw new IsFinished();
        }
    }

    public synchronized boolean isFinished() {
        return this.messages.isEmpty() && this.closed;
    }

    public synchronized void close() {
        this.closed = true;
        this.notifyAll();
    }
}

package org.jdepoix.testrelationfinder.concurrency;

public abstract class Consumer<T> implements Runnable {
    private final Channel<T> channel;

    public Consumer(Channel<T> channel) {
        this.channel = channel;
    }

    public abstract void consumeMessage(T message);

    @Override
    public void run() {
        while(!channel.isFinished()) {
            try {
                this.consumeMessage(channel.receive());
            } catch (IsFinished isFinished) {}
        }
    }
}

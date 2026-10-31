package com.example.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public final class Client implements AutoCloseable {
    private final int QUEUE_CAPACITY = 100;

    private final Socket socket;
    private BlockingQueue<String> queue;
    private BufferedReader in;
    private PrintWriter out;
    private int id;
    private volatile Instant lastReply;

    protected Client(Socket socket, BufferedReader in, PrintWriter out, int id) {
        this.socket = socket;
        this.queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        this.in = in;
        this.out = out;
        this.id = id;
        this.lastReply = Instant.now();
    }

    protected int getId() {
        return this.id;
    }

    protected BufferedReader getIn() {
        return this.in;
    }

    protected PrintWriter getOut() {
        return this.out;
    }

    protected synchronized void addToQueue(String message) {
        this.queue.offer(message);
    }

    protected void setLastReply() {
        this.lastReply = Instant.now();
    }

    public Instant getLastReply() {
        return this.lastReply;
    }

    protected boolean isClosed() {
        return this.socket.isClosed();
    }

    protected void disconnect() {
        try {
            this.socket.close();
        } catch (IOException e) {
        }
    }

    protected void writeLoop() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                this.write();
                this.flush();
                Thread.sleep(10);
            }
        } catch (InterruptedException e) {
        }
    }

    private void write() {
        while (true) {
            if (this.queue.isEmpty()) {
                break;
            }

            try {
                var message = this.queue.take();
                this.out.write(message);
            } catch (Exception e) {
                System.err.println(e);
            }
        }
    }

    protected void flush() {
        this.out.flush();
    }

    @Override
    public void close() throws IOException {
        this.queue.clear();
        this.socket.close();
    }
}

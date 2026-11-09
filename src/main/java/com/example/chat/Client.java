package com.example.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public final class Client implements AutoCloseable {
    private final int QUEUE_CAPACITY = 100;

    private final Socket socket;
    private BlockingQueue<Message> queue;
    private final BufferedReader in;
    private final PrintWriter out;
    private int id;
    private volatile Instant lastReply;
    private String nickname;
    private Room room;

    protected Client(Socket socket, int id) throws IOException {
        this.socket = socket;
        this.queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        this.id = id;
        this.lastReply = Instant.now();
        this.nickname = "client-" + id;
    }

    protected int getId() {
        return this.id;
    }

    protected String getNickname() {
        return this.nickname;
    }

    protected void setNickname(String nickname) {
        this.nickname = nickname;
    }

    protected Room getRoom() {
        return this.room;
    }

    protected void setRoom(Room room) {
        this.room = room;
    }

    protected String readLine() throws IOException {
        return this.in.readLine();
    }

    protected synchronized void addToQueue(Message message) {
        this.queue.offer(message);
    }

    protected void send(String text) {
        addToQueue(Message.format(text));
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

    // runtime of the whole process of adding chat messages
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
                this.out.write(message.format());
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

package com.example.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;

public final class Client implements AutoCloseable {

    private Socket socket;
    private BufferedReader in;
    private int id;

    protected Client(BufferedReader in, Socket socket, int id) {
        this.socket = socket;
        this.in = in;
        this.id = id;
    }

    protected int getId() {
        return this.id;
    }

    protected BufferedReader getIn() {
        return this.in;
    }

    @Override
    public void close() throws IOException {
        this.socket.close();
        this.in.close();
    }
}

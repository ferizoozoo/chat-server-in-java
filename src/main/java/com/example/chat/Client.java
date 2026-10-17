package com.example.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;

public final class Client implements AutoCloseable {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private int id;

    protected Client(BufferedReader in, PrintWriter out, Socket socket, int id) {
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.id = id;
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

    @Override
    public void close() throws IOException {
        this.socket.close();
        this.in.close();
        this.out.close();
    }
}

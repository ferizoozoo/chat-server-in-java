package com.example.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Hello world!
 */
public class ChatServer {
    private int port;
    // private String host;

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    // public String getHost() {
    // return this.host;
    // }

    // public void setHost(String host) {
    // this.host = host;
    // }

    public void run() throws IOException {
        try (ServerSocket server = new ServerSocket(this.port)) {
            System.out.println("Server is running on " + this.port);
            while (true) {
                Socket client = server.accept();
                Thread.ofVirtual().start(() -> handleConnection(client));
            }
        }
    }

    private void handleConnection(Socket client) {
        try (client;
                var in = new BufferedReader(new InputStreamReader(
                        client.getInputStream(), StandardCharsets.UTF_8));
                var out = new PrintWriter(new OutputStreamWriter(
                        client.getOutputStream(), StandardCharsets.UTF_8), true)) {

            String line;
            while ((line = in.readLine()) != null) { // null = client disconnected
                out.println("echo: " + line);
            }
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}

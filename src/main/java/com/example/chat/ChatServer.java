package com.example.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private int port;
    private Set<Client> clients = ConcurrentHashMap.newKeySet();
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
                Socket clientSocket = server.accept();
                var in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                Client client = new Client(in, clientSocket, clients.size() + 1);
                clients.add(client);
                Thread.ofVirtual().start(() -> handleConnection(client));
            }
        }
    }

    private void handleConnection(Client client) {
        try (client) {
            String line;
            while ((line = client.getIn().readLine()) != null) { // null = client disconnected
                System.out.println("client " + client.getId() + ": " + line);
            }
        } catch (IOException e) {
            System.out.println("Client could not be initiated properly");
        }
    }
}

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

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void run() throws IOException {
        try (ServerSocket server = new ServerSocket(this.port)) {
            System.out.println("Server is running on " + this.port);
            while (true) {
                Socket clientSocket = server.accept();
                var in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                var out = new PrintWriter(new OutputStreamWriter(
                        clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);
                Client client = new Client(in, out, clientSocket, clients.size() + 1);
                clients.add(client);
                Thread.ofVirtual().start(() -> handleConnection(client));
            }
        }
    }

    private void handleConnection(Client client) {
        try (client) {
            String line;
            String nickname = "client-" + client.getId();
            while ((line = client.getIn().readLine()) != null) { // null = client disconnected
                var command = CommandParser.parse(line);
                if (command.command() == Commands.NICK) {
                    nickname = command.value();
                    continue;
                }
                final String message = "[" + nickname + "] " + line;
                this.clients.forEach(c -> c.getOut().println(message));
            }
        } catch (IOException e) {
            System.out.println("Client could not be initiated properly");
        }
    }
}

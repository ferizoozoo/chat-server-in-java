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
    private ConcurrentHashMap<String, Set<Client>> rooms = new ConcurrentHashMap<>();

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
                final int clientCount = this.rooms.values().stream().mapToInt(Set::size).sum();
                Client client = new Client(in, out, clientSocket, clientCount + 1);
                Thread.ofVirtual().start(() -> handleConnection(client));
            }
        }
    }

    private void handleConnection(Client client) {
        try (client) {
            String line;
            String nickname = "client-" + client.getId();
            String roomname = "";
            while ((line = client.getIn().readLine()) != null) { // null = client disconnected
                var command = CommandParser.parse(line);
                if (command.command() == Commands.NICK) {
                    nickname = command.value();
                    continue;
                } else if (command.command() == Commands.JOIN) {
                    roomname = command.value();
                    rooms.computeIfAbsent(roomname, k -> ConcurrentHashMap.newKeySet()).add(client);
                    continue;
                }
                final String message = "[" + nickname + "-" + roomname + "] " + line;
                this.rooms.get(roomname).forEach(k -> k.getOut().println(message));
            }
        } catch (IOException e) {
            System.out.println("Client could not be initiated properly");
        }
    }
}

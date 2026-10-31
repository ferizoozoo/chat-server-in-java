package com.example.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    static final int IDLE_TIMEOUT_MS = 30_000;
    static final int HEARTBEAT_INTERVAL_MS = 10_000;

    private int port;
    private int idleTimeoutMs = IDLE_TIMEOUT_MS;
    private int heartbeatIntervalMs = HEARTBEAT_INTERVAL_MS;
    private ConcurrentHashMap<String, Set<Client>> rooms = new ConcurrentHashMap<>();

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setIdleTimeoutMs(int idleTimeoutMs) {
        this.idleTimeoutMs = idleTimeoutMs;
    }

    public void setHeartbeatIntervalMs(int heartbeatIntervalMs) {
        this.heartbeatIntervalMs = heartbeatIntervalMs;
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
                Client client = new Client(clientSocket, in, out, clientCount + 1);
                Thread.ofVirtual().start(() -> handleConnection(client));
                Thread.ofVirtual().start(() -> heartbeat(client));
            }
        }
    }

    private void handleConnection(Client client) {
        Thread writer = Thread.ofVirtual().start(() -> client.writeLoop());
        String roomname = "";
        try (client) {
            String line;
            String nickname = "client-" + client.getId();
            while ((line = client.getIn().readLine()) != null) { // null = client disconnected
                client.setLastReply();
                var command = CommandParser.parse(line);
                if (command.command() == Commands.NICK) {
                    nickname = command.value();
                    continue;
                } else if (command.command() == Commands.JOIN) {
                    roomname = command.value();
                    rooms.computeIfAbsent(roomname, k -> ConcurrentHashMap.newKeySet()).add(client);
                    continue;
                } else if (command.command() == Commands.PONG) {
                    continue;
                } else if (command.command() == Commands.PING) {
                    client.addToQueue(Message.format("/pong"));
                    continue;
                }
                final Message message = Message.format(nickname, roomname, line);
                this.rooms.get(roomname).forEach(k -> k.addToQueue(message));
            }
        } catch (IOException e) {
            System.out.println("Client " + client.getId() + " disconnected: " + e.getMessage());
        } finally {
            writer.interrupt();
            var room = this.rooms.get(roomname);
            if (room != null) {
                room.remove(client);
            }
        }
    }

    private void heartbeat(Client client) {
        var timeout = Duration.ofMillis(this.idleTimeoutMs);
        try {
            while (!client.isClosed()) {
                Thread.sleep(this.heartbeatIntervalMs);
                var silence = Duration.between(client.getLastReply(), Instant.now());
                if (silence.compareTo(timeout) > 0) {
                    System.out.println("Client " + client.getId() + " timed out after " + silence.toMillis() + "ms");
                    client.disconnect();
                    return;
                }
                client.addToQueue(Message.format("/ping"));
            }
        } catch (InterruptedException e) {
            client.disconnect();
        }
    }
}

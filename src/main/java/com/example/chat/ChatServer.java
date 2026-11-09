package com.example.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatServer {
    static final int IDLE_TIMEOUT_MS = 30_000;
    static final int HEARTBEAT_INTERVAL_MS = 10_000;

    private int port;
    private int idleTimeoutMs = IDLE_TIMEOUT_MS;
    private int heartbeatIntervalMs = HEARTBEAT_INTERVAL_MS;
    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Client> users = new ConcurrentHashMap<>();
    private final AtomicInteger nextClientId = new AtomicInteger(1);

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
        try (var server = new ServerSocket(this.port)) {
            System.out.println("Server is running on " + this.port);
            while (true) {
                var socket = server.accept();
                Client client;
                try {
                    client = new Client(socket, this.nextClientId.getAndIncrement());
                } catch (IOException e) {
                    System.out.println("Could not set up client: " + e.getMessage());
                    socket.close();
                    continue;
                }
                Thread.ofVirtual().start(() -> handleConnection(client));
                Thread.ofVirtual().start(() -> heartbeat(client));
            }
        }
    }

    private void handleConnection(Client client) {
        var writer = Thread.ofVirtual().start(client::writeLoop);
        try (client) {
            readLoop(client);
        } catch (IOException e) {
            System.out.println("Client " + client.getId() + " disconnected: " + e.getMessage());
        } finally {
            writer.interrupt();
            this.rooms.values().forEach(room -> room.leave(client));
            this.users.values().removeIf(user -> user == client);
        }
    }

    private void readLoop(Client client) throws IOException {
        this.users.put(client.getNickname(), client);

        String line;
        while ((line = client.readLine()) != null) { // null = client disconnected
            client.setLastReply();
            var command = CommandParser.parse(line);
            switch (command.command()) {
                case NICK -> changeNickname(client, command.arg());
                case JOIN -> joinRoom(client, command.arg());
                case MSG -> sendDirectMessage(client, command.arg(), command.subArg());
                case ROOMS -> {
                    String names = this.rooms.reduceKeys(Long.MAX_VALUE, (a, b) -> a + ", " + b);
                    client.send(names == null ? "No rooms yet." : names);
                }
                case WHO -> client.send(client.getRoom() == null ? "" : client.getRoom().getName());
                case PING -> client.send("/pong");
                case PONG -> {
                }
                case ERROR -> client.send(command.arg());
                case QUIT -> {
                    return;
                }
                default -> sendToRoom(client, line);
            }
        }
    }

    private void changeNickname(Client client, String newName) {
        if (newName.isEmpty()) {
            client.send("Usage: /nick <name>");
        } else if (newName.equals(client.getNickname())) {
            return;
        } else if (this.users.putIfAbsent(newName, client) != null) {
            client.send("Nickname '" + newName + "' is already taken.");
        } else {
            this.users.remove(client.getNickname(), client);
            client.setNickname(newName);
        }
    }

    private void joinRoom(Client client, String name) {
        if (name.isEmpty()) {
            client.send("Usage: /join <room>");
            return;
        }
        var room = this.rooms.computeIfAbsent(name, Room::new);
        client.setRoom(room);
        room.join(client);
    }

    private void sendDirectMessage(Client client, String target, String text) {
        if (target.isEmpty() || text.isEmpty()) {
            client.send("Usage: /msg <nick> <text>");
            return;
        }
        var user = this.users.get(target);
        if (user == null) {
            client.send("No user named '" + target + "'.");
            return;
        }
        var message = Message.format(client.getNickname(), "dm", text);
        user.addToQueue(message);
        if (user != client) {
            client.addToQueue(message);
        }
    }

    private void sendToRoom(Client client, String text) {
        var room = client.getRoom();
        if (room == null) {
            client.send("You are not in a room. Use /join <room> first.");
            return;
        }
        room.broadcast(Message.format(client.getNickname(), room.getName(), text));
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
                client.send("/ping");
            }
        } catch (InterruptedException e) {
            client.disconnect();
        }
    }
}

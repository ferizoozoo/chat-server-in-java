package com.example.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatServer {
    static final int IDLE_TIMEOUT_MS = 30_000;
    static final int HEARTBEAT_INTERVAL_MS = 10_000;

    private int port;
    private int idleTimeoutMs = IDLE_TIMEOUT_MS;
    private int heartbeatIntervalMs = HEARTBEAT_INTERVAL_MS;
    private ConcurrentHashMap<String, Set<Client>> rooms = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Client> users = new ConcurrentHashMap<>();
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
        try (ServerSocket server = new ServerSocket(this.port)) {
            System.out.println("Server is running on " + this.port);
            while (true) {
                Socket clientSocket = server.accept();
                Client client;
                try {
                    client = new Client(clientSocket, nextClientId.getAndIncrement());
                } catch (IOException e) {
                    System.out.println("Could not set up client: " + e.getMessage());
                    clientSocket.close();
                    continue;
                }
                Thread.ofVirtual().start(() -> handleConnection(client));
                Thread.ofVirtual().start(() -> heartbeat(client));
            }
        }
    }

    private void handleConnection(Client client) {
        Thread writer = Thread.ofVirtual().start(() -> client.writeLoop());
        try (client) {
            readLoop(client);
        } catch (IOException e) {
            System.out.println("Client " + client.getId() + " disconnected: " + e.getMessage());
        } finally {
            writer.interrupt();
            this.rooms.values().forEach(room -> room.remove(client));
            this.users.values().removeIf(user -> user == client);
        }
    }

    private void readLoop(Client client) throws IOException {
        String line;
        String nickname = "client-" + client.getId();
        String roomname = "";
        this.users.put(nickname, client);
        while ((line = client.readLine()) != null) { // null = client disconnected
            client.setLastReply();
            var command = CommandParser.parse(line);
            switch (command.command()) {
                case Commands.NICK -> {
                    String newName = command.arg();
                    if (newName.isEmpty()) {
                        client.addToQueue(Message.format("Usage: /nick <name>"));
                    } else if (newName.equals(nickname)) {
                    } else if (this.users.putIfAbsent(newName, client) != null) {
                        client.addToQueue(Message.format("Nickname '" + newName + "' is already taken."));
                    } else {
                        this.users.remove(nickname, client);
                        nickname = newName;
                    }
                }
                case Commands.JOIN -> {
                    roomname = command.arg();
                    rooms.computeIfAbsent(roomname, k -> ConcurrentHashMap.newKeySet()).add(client);
                }
                case Commands.PING -> {
                    client.addToQueue(Message.format("/pong"));
                }
                case Commands.PONG -> {
                }
                case Commands.QUIT -> {
                    return;
                }
                case Commands.WHO -> {
                    client.addToQueue(Message.format(roomname));
                }
                case Commands.MSG -> {
                    String target = command.arg();
                    String text = command.subArg();
                    var user = this.users.get(target);
                    if (target.isEmpty() || text.isEmpty()) {
                        client.addToQueue(Message.format("Usage: /msg <nick> <text>"));
                    } else if (user == null) {
                        client.addToQueue(Message.format("No user named '" + target + "'."));
                    } else {
                        final Message message = Message.format(nickname, "dm", text);
                        user.addToQueue(message);
                        if (user != client) {
                            client.addToQueue(message);
                        }
                    }
                }
                case Commands.ERROR -> {
                    client.addToQueue(Message.format(command.arg()));
                }
                case Commands.ROOMS -> {
                    String names = this.rooms.reduceKeys(Long.MAX_VALUE, (a, b) -> a + ", " + b);
                    client.addToQueue(Message.format(names == null ? "No rooms yet." : names));
                }
                default -> {
                    if (roomname.isEmpty()) {
                        client.addToQueue(Message.format("You are not in a room. Use /join <room> first."));
                    } else {
                        final Message message = Message.format(nickname, roomname, line);
                        this.rooms.get(roomname).forEach(k -> k.addToQueue(message));
                    }
                }
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

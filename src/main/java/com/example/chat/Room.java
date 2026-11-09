package com.example.chat;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public final class Room {
    static final int HISTORY_SIZE = 50;

    private final String name;
    private final Set<Client> members = new HashSet<>();
    private final ArrayDeque<Message> history = new ArrayDeque<>();

    public Room(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public synchronized void join(Client client) {
        if (this.members.add(client)) {
            this.history.forEach(client::addToQueue);
        }
    }

    public synchronized void leave(Client client) {
        this.members.remove(client);
    }

    public synchronized void broadcast(Message message) {
        this.history.addLast(message);
        if (this.history.size() > HISTORY_SIZE) {
            this.history.removeFirst();
        }
        this.members.forEach(member -> member.addToQueue(message));
    }
}

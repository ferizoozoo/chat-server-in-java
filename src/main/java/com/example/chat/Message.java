package com.example.chat;

import java.time.Instant;
import java.util.Objects;

public record Message(String sender, String room, String text, Instant createdAt) {

    public Message {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public static Message format(String sender, String room, String text) {
        return new Message(Objects.requireNonNull(sender, "sender"), Objects.requireNonNull(room, "room"), text,
                Instant.now());
    }

    public static Message format(String text) {
        return new Message(null, null, text, Instant.now());
    }

    public boolean isSystem() {
        return this.sender == null;
    }

    public String format() {
        if (isSystem()) {
            return this.text + "\n";
        }
        return "[" + this.sender + "-" + this.room + " " + this.createdAt + "] " + this.text + "\n";
    }
}

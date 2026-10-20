package com.example.chat;

public class CommandParser {

    public static CommandValue parse(String line) {
        if (line.startsWith("/")) {
            String[] parts = line.substring(1).split(" ", 2);
            String cmd = parts[0];
            String arg = parts.length > 1 ? parts[1].strip() : "";

            switch (cmd) {
                case "nick" -> {
                    return new CommandValue(Commands.NICK, arg);
                }
                case "join" -> {
                    return new CommandValue(Commands.JOIN, arg);
                }
                case "quit" -> {
                    return new CommandValue(Commands.QUIT, arg);
                }
            }
        }

        return new CommandValue(Commands.NOOP, "");

    }

}

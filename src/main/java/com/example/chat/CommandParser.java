package com.example.chat;

public class CommandParser {

    public static CommandValue parse(String line) {
        if (line.startsWith("/")) {
            String[] parts = line.substring(1).strip().split("\\s+", 3);
            String cmd = parts[0];
            String arg = parts.length > 1 ? parts[1].strip() : "";
            String subArg = parts.length > 2 ? parts[2].strip() : "";

            switch (cmd) {
                case "nick" -> {
                    if (!subArg.isEmpty()) {
                        return new CommandValue(Commands.ERROR, null, "Nicknames can't contain spaces.", "");
                    }
                    return new CommandValue(Commands.NICK, null, arg, "");
                }
                case "join" -> {
                    if (!subArg.isEmpty()) {
                        return new CommandValue(Commands.ERROR, null, "Room names can't contain spaces.", "");
                    }
                    return new CommandValue(Commands.JOIN, null, arg, "");
                }
                case "quit" -> {
                    return new CommandValue(Commands.QUIT, null, arg, "");
                }
                case "ping" -> {
                    return new CommandValue(Commands.PING, null, arg, "");
                }
                case "pong" -> {
                    return new CommandValue(Commands.PONG, null, arg, "");
                }
                case "who" -> {
                    return new CommandValue(Commands.WHO, null, arg, "");
                }
                case "rooms" -> {
                    return new CommandValue(Commands.ROOMS, null, arg, "");
                }
                case "msg" -> {
                    return new CommandValue(Commands.MSG, Commands.NICK, arg, subArg);
                }
            }
        }

        return new CommandValue(Commands.NOOP, null, "", "");

    }
}

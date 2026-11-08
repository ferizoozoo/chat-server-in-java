package com.example.chat;

public record CommandValue(Commands command, Commands subCommands, String arg, String subArg) {
}

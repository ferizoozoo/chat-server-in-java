package com.example.chat;

import java.io.IOException;

public class App {
    public static void main(String[] args) {
        ChatServer cs = new ChatServer();
        cs.setPort(5000);
        try {
            cs.run();
        } catch (IOException e) {
            System.out.println("Could not open server");
        }
    }
}

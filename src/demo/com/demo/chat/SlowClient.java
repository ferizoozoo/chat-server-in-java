package com.demo.chat;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.PrintWriter;

public class SlowClient {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5000;
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("localhost", port));
            PrintWriter out = new PrintWriter(s.getOutputStream(), true);
            out.println("/nick slow");
            out.println("/join x");
            System.out.println("slow client joined 'test', never reading...");
            Thread.sleep(Long.MAX_VALUE);
        }
    }
}

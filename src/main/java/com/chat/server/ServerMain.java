package com.chat.server;

import com.chat.common.util.LoggerUtil;
import com.chat.server.network.ChatServer;

public class ServerMain {

    public static void main(String[] args) {
        LoggerUtil.info("Starting Chat Application Server...");
        ChatServer server = new ChatServer();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LoggerUtil.info("Shutdown signal received. Cleaning up resources...");
            server.stop();
        }));

        server.start();
    }
}

package com.chat.server.network;

import com.chat.common.util.LoggerUtil;
import com.chat.server.config.ServerConfig;
import com.chat.server.db.DatabaseConnectionManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {

    private final int port;
    private final ExecutorService threadPool;
    private ServerSocket serverSocket;
    private WebChatServer webChatServer;
    private volatile boolean running = false;

    public ChatServer() {
        ServerConfig config = ServerConfig.getInstance();
        this.port = config.getPort();
        this.threadPool = Executors.newFixedThreadPool(config.getMaxThreadPoolSize());
        this.webChatServer = new WebChatServer();
    }

    public void start() {
        // Initialize Database schema on startup
        DatabaseConnectionManager.getInstance();

        // Start Web Preview Portal on http://localhost:8080
        webChatServer.start();

        try {
            serverSocket = new ServerSocket(port);
            running = true;
            LoggerUtil.info("==================================================");
            LoggerUtil.info("      ANTIGRAVITY CHAT SERVER STARTED            ");
            LoggerUtil.info(" Listening on Port: " + port);
            LoggerUtil.info(" Max Threads: " + ServerConfig.getInstance().getMaxThreadPoolSize());
            LoggerUtil.info(" Storage Dir: " + ServerConfig.getInstance().getStorageDirectory());
            LoggerUtil.info("==================================================");

            while (running) {
                Socket clientSocket = serverSocket.accept();
                LoggerUtil.info("New TCP client connection accepted from: " + clientSocket.getRemoteSocketAddress());
                ClientHandler handler = new ClientHandler(clientSocket);
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            if (running) {
                LoggerUtil.error("ChatServer exception encountered", e);
            } else {
                LoggerUtil.info("ChatServer socket closed.");
            }
        } finally {
            stop();
        }
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;
        LoggerUtil.info("Shutting down Antigravity Chat Server...");
        if (webChatServer != null) {
            webChatServer.stop();
        }
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            LoggerUtil.error("Error closing ServerSocket", e);
        }
        threadPool.shutdownNow();
        LoggerUtil.info("ChatServer stopped.");
    }
}

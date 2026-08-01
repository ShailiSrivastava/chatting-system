package com.chat.server.config;

import com.chat.common.util.Constants;

public class ServerConfig {
    private static ServerConfig instance;

    private int port;
    private String storageDirectory;
    private int maxThreadPoolSize;

    private ServerConfig() {
        this.port = Constants.DEFAULT_SERVER_PORT;
        this.storageDirectory = Constants.SERVER_STORAGE_DIR;
        this.maxThreadPoolSize = 50;
    }

    public static synchronized ServerConfig getInstance() {
        if (instance == null) {
            instance = new ServerConfig();
        }
        return instance;
    }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getStorageDirectory() { return storageDirectory; }
    public void setStorageDirectory(String storageDirectory) { this.storageDirectory = storageDirectory; }

    public int getMaxThreadPoolSize() { return maxThreadPoolSize; }
    public void setMaxThreadPoolSize(int maxThreadPoolSize) { this.maxThreadPoolSize = maxThreadPoolSize; }
}

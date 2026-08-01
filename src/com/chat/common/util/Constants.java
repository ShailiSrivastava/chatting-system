package com.chat.common.util;

public class Constants {
    public static final int DEFAULT_SERVER_PORT = 8888;
    public static final String DEFAULT_SERVER_HOST = "localhost";
    public static final String SERVER_STORAGE_DIR = "server_storage";
    public static final String CLIENT_DOWNLOAD_DIR = "client_downloads";

    public static final String AES_SECRET_KEY = "AntigravityCoreJavaKey"; // 16 bytes key for AES-128
    public static final int SALT_LENGTH = 16;
    public static final int MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024; // 50MB max file size
}

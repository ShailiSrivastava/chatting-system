package com.chat.server.db;

import com.chat.common.util.LoggerUtil;
import com.chat.server.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnectionManager {

    private static DatabaseConnectionManager instance;

    private DatabaseConnectionManager() {
        try {
            Class.forName(DatabaseConfig.JDBC_DRIVER);
            LoggerUtil.info("JDBC Driver loaded successfully: " + DatabaseConfig.JDBC_DRIVER);
            initializeDatabaseSchema();
        } catch (ClassNotFoundException e) {
            LoggerUtil.error("Failed to load JDBC driver", e);
            throw new RuntimeException(e);
        }
    }

    public static synchronized DatabaseConnectionManager getInstance() {
        if (instance == null) {
            instance = new DatabaseConnectionManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                DatabaseConfig.JDBC_URL,
                DatabaseConfig.JDBC_USER,
                DatabaseConfig.JDBC_PASSWORD
        );
    }

    private void initializeDatabaseSchema() {
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "username VARCHAR(50) NOT NULL UNIQUE, " +
                "email VARCHAR(100) NOT NULL UNIQUE, " +
                "password_hash VARCHAR(255) NOT NULL, " +
                "salt VARCHAR(255) NOT NULL, " +
                "status VARCHAR(20) DEFAULT 'OFFLINE', " +
                "bio VARCHAR(255) DEFAULT '', " +
                "avatar_path VARCHAR(255) DEFAULT '', " +
                "last_active_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";

        String createMessagesTable = "CREATE TABLE IF NOT EXISTS messages (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "sender_id BIGINT NOT NULL, " +
                "receiver_id BIGINT, " +
                "group_id BIGINT, " +
                "content TEXT NOT NULL, " +
                "message_type VARCHAR(20) DEFAULT 'TEXT', " +
                "status VARCHAR(20) DEFAULT 'SENT', " +
                "is_encrypted BOOLEAN DEFAULT FALSE, " +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (sender_id) REFERENCES users(id)" +
                ");";

        String createGroupsTable = "CREATE TABLE IF NOT EXISTS chat_groups (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL, " +
                "description VARCHAR(255), " +
                "created_by BIGINT NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (created_by) REFERENCES users(id)" +
                ");";

        String createGroupMembersTable = "CREATE TABLE IF NOT EXISTS group_members (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "group_id BIGINT NOT NULL, " +
                "user_id BIGINT NOT NULL, " +
                "role VARCHAR(20) DEFAULT 'MEMBER', " +
                "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (group_id) REFERENCES chat_groups(id), " +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ");";

        String createSharedFilesTable = "CREATE TABLE IF NOT EXISTS shared_files (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "message_id BIGINT, " +
                "original_name VARCHAR(255) NOT NULL, " +
                "stored_name VARCHAR(255) NOT NULL, " +
                "file_size_bytes BIGINT NOT NULL, " +
                "file_type VARCHAR(50), " +
                "upload_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";

        String createSessionsTable = "CREATE TABLE IF NOT EXISTS user_sessions (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id BIGINT NOT NULL, " +
                "session_token VARCHAR(255) NOT NULL UNIQUE, " +
                "login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "last_seen_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "is_active BOOLEAN DEFAULT TRUE, " +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ");";

        String createNotificationsTable = "CREATE TABLE IF NOT EXISTS notifications (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id BIGINT NOT NULL, " +
                "title VARCHAR(100) NOT NULL, " +
                "content VARCHAR(255) NOT NULL, " +
                "is_read BOOLEAN DEFAULT FALSE, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createMessagesTable);
            stmt.execute(createGroupsTable);
            stmt.execute(createGroupMembersTable);
            stmt.execute(createSharedFilesTable);
            stmt.execute(createSessionsTable);
            stmt.execute(createNotificationsTable);
            LoggerUtil.info("Database schema initialized successfully.");
        } catch (SQLException e) {
            LoggerUtil.error("Failed to initialize database schema", e);
        }
    }
}

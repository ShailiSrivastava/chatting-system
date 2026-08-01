package com.chat.server.config;

public class DatabaseConfig {
    public static final String JDBC_DRIVER = "org.h2.Driver";
    public static final String JDBC_URL = "jdbc:h2:./chatdb;DB_CLOSE_DELAY=-1;MODE=MySQL;AUTO_SERVER=TRUE";
    public static final String JDBC_USER = "sa";
    public static final String JDBC_PASSWORD = "";
}

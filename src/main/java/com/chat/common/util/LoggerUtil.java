package com.chat.common.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LoggerUtil {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static synchronized void info(String message) {
        System.out.printf("[%s] [INFO] [%s] %s%n", sdf.format(new Date()), Thread.currentThread().getName(), message);
    }

    public static synchronized void warn(String message) {
        System.out.printf("[%s] [WARN] [%s] %s%n", sdf.format(new Date()), Thread.currentThread().getName(), message);
    }

    public static synchronized void error(String message) {
        System.err.printf("[%s] [ERROR] [%s] %s%n", sdf.format(new Date()), Thread.currentThread().getName(), message);
    }

    public static synchronized void error(String message, Throwable throwable) {
        System.err.printf("[%s] [ERROR] [%s] %s: %s%n", sdf.format(new Date()), Thread.currentThread().getName(), message, throwable.getMessage());
        throwable.printStackTrace(System.err);
    }
}

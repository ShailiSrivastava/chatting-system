package com.chat.server.dao.impl;

import com.chat.common.model.Notification;
import com.chat.common.util.LoggerUtil;
import com.chat.server.dao.NotificationDAO;
import com.chat.server.db.DatabaseConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAOImpl implements NotificationDAO {

    private final DatabaseConnectionManager dbManager = DatabaseConnectionManager.getInstance();

    @Override
    public boolean createNotification(Notification notification) {
        String sql = "INSERT INTO notifications (user_id, title, content, is_read, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, notification.getUserId());
            pstmt.setString(2, notification.getTitle());
            pstmt.setString(3, notification.getContent());
            pstmt.setBoolean(4, notification.isRead());
            pstmt.setTimestamp(5, notification.getCreatedAt() != null ? notification.getCreatedAt() : new Timestamp(System.currentTimeMillis()));

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) notification.setId(rs.getLong(1));
                }
                return true;
            }
        } catch (SQLException e) {
            LoggerUtil.error("Error creating notification for user " + notification.getUserId(), e);
        }
        return false;
    }

    @Override
    public List<Notification> getUserNotifications(Long userId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Notification n = new Notification();
                    n.setId(rs.getLong("id"));
                    n.setUserId(rs.getLong("user_id"));
                    n.setTitle(rs.getString("title"));
                    n.setContent(rs.getString("content"));
                    n.setRead(rs.getBoolean("is_read"));
                    n.setCreatedAt(rs.getTimestamp("created_at"));
                    list.add(n);
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("Error fetching notifications for user " + userId, e);
        }
        return list;
    }

    @Override
    public boolean markAsRead(Long notificationId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, notificationId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.error("Error marking notification as read " + notificationId, e);
        }
        return false;
    }
}

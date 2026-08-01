package com.chat.server.dao.impl;

import com.chat.common.model.Message;
import com.chat.common.model.MessageStatus;
import com.chat.common.model.MessageType;
import com.chat.common.util.LoggerUtil;
import com.chat.server.dao.MessageDAO;
import com.chat.server.db.DatabaseConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAOImpl implements MessageDAO {

    private final DatabaseConnectionManager dbManager = DatabaseConnectionManager.getInstance();

    @Override
    public boolean saveMessage(Message message) {
        String sql = "INSERT INTO messages (sender_id, receiver_id, group_id, content, message_type, status, is_encrypted, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, message.getSenderId());
            if (message.getReceiverId() != null) {
                pstmt.setLong(2, message.getReceiverId());
            } else {
                pstmt.setNull(2, Types.BIGINT);
            }
            if (message.getGroupId() != null) {
                pstmt.setLong(3, message.getGroupId());
            } else {
                pstmt.setNull(3, Types.BIGINT);
            }
            pstmt.setString(4, message.getContent());
            pstmt.setString(5, message.getMessageType() != null ? message.getMessageType().name() : MessageType.TEXT.name());
            pstmt.setString(6, message.getStatus() != null ? message.getStatus().name() : MessageStatus.SENT.name());
            pstmt.setBoolean(7, message.isEncrypted());
            pstmt.setTimestamp(8, message.getTimestamp() != null ? message.getTimestamp() : new Timestamp(System.currentTimeMillis()));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        message.setId(rs.getLong(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            LoggerUtil.error("Error saving message", e);
        }
        return false;
    }

    @Override
    public List<Message> getPrivateChatHistory(Long userId1, Long userId2) {
        List<Message> list = new ArrayList<>();
        String sql = "SELECT m.*, u.username as sender_username FROM messages m " +
                "JOIN users u ON m.sender_id = u.id " +
                "WHERE (m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?) " +
                "ORDER BY m.timestamp ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId1);
            pstmt.setLong(2, userId2);
            pstmt.setLong(3, userId2);
            pstmt.setLong(4, userId1);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToMessage(rs));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("Error getting private chat history", e);
        }
        return list;
    }

    @Override
    public List<Message> getGroupChatHistory(Long groupId) {
        List<Message> list = new ArrayList<>();
        String sql = "SELECT m.*, u.username as sender_username FROM messages m " +
                "JOIN users u ON m.sender_id = u.id " +
                "WHERE m.group_id = ? ORDER BY m.timestamp ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, groupId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToMessage(rs));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("Error getting group chat history for group " + groupId, e);
        }
        return list;
    }

    @Override
    public List<Message> getPendingOfflineMessages(Long receiverId) {
        List<Message> list = new ArrayList<>();
        String sql = "SELECT m.*, u.username as sender_username FROM messages m " +
                "JOIN users u ON m.sender_id = u.id " +
                "WHERE m.receiver_id = ? AND m.status = 'SENT' ORDER BY m.timestamp ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, receiverId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToMessage(rs));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("Error getting pending offline messages for user " + receiverId, e);
        }
        return list;
    }

    @Override
    public boolean updateMessageStatus(Long messageId, MessageStatus status) {
        String sql = "UPDATE messages SET status = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setLong(2, messageId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.error("Error updating message status for message " + messageId, e);
        }
        return false;
    }

    @Override
    public boolean markMessagesAsDelivered(Long receiverId) {
        String sql = "UPDATE messages SET status = 'DELIVERED' WHERE receiver_id = ? AND status = 'SENT'";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, receiverId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.error("Error marking messages as delivered for receiver " + receiverId, e);
        }
        return false;
    }

    @Override
    public boolean markMessagesAsRead(Long senderId, Long receiverId) {
        String sql = "UPDATE messages SET status = 'READ' WHERE sender_id = ? AND receiver_id = ? AND status IN ('SENT', 'DELIVERED')";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, senderId);
            pstmt.setLong(2, receiverId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.error("Error marking messages as read between " + senderId + " and " + receiverId, e);
        }
        return false;
    }

    @Override
    public List<Message> searchChatHistory(Long userId, String keyword) {
        List<Message> list = new ArrayList<>();
        String sql = "SELECT m.*, u.username as sender_username FROM messages m " +
                "JOIN users u ON m.sender_id = u.id " +
                "WHERE (m.sender_id = ? OR m.receiver_id = ?) AND LOWER(m.content) LIKE LOWER(?) " +
                "ORDER BY m.timestamp DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.setLong(2, userId);
            pstmt.setString(3, "%" + keyword + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToMessage(rs));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("Error searching chat history for user " + userId, e);
        }
        return list;
    }

    private Message mapRowToMessage(ResultSet rs) throws SQLException {
        Message msg = new Message();
        msg.setId(rs.getLong("id"));
        msg.setSenderId(rs.getLong("sender_id"));
        msg.setSenderUsername(rs.getString("sender_username"));

        long rId = rs.getLong("receiver_id");
        if (!rs.wasNull()) msg.setReceiverId(rId);

        long gId = rs.getLong("group_id");
        if (!rs.wasNull()) msg.setGroupId(gId);

        msg.setContent(rs.getString("content"));
        msg.setMessageType(MessageType.valueOf(rs.getString("message_type")));
        msg.setStatus(MessageStatus.valueOf(rs.getString("status")));
        msg.setEncrypted(rs.getBoolean("is_encrypted"));
        msg.setTimestamp(rs.getTimestamp("timestamp"));
        return msg;
    }
}

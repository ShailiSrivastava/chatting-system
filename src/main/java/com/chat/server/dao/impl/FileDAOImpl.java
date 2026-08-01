package com.chat.server.dao.impl;

import com.chat.common.model.SharedFile;
import com.chat.common.util.LoggerUtil;
import com.chat.server.dao.FileDAO;
import com.chat.server.db.DatabaseConnectionManager;

import java.sql.*;

public class FileDAOImpl implements FileDAO {

    private final DatabaseConnectionManager dbManager = DatabaseConnectionManager.getInstance();

    @Override
    public boolean saveFileMetadata(SharedFile sharedFile) {
        String sql = "INSERT INTO shared_files (message_id, original_name, stored_name, file_size_bytes, file_type, upload_timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (sharedFile.getMessageId() != null) {
                pstmt.setLong(1, sharedFile.getMessageId());
            } else {
                pstmt.setNull(1, Types.BIGINT);
            }
            pstmt.setString(2, sharedFile.getOriginalName());
            pstmt.setString(3, sharedFile.getStoredName());
            pstmt.setLong(4, sharedFile.getFileSizeBytes());
            pstmt.setString(5, sharedFile.getFileType());
            pstmt.setTimestamp(6, sharedFile.getUploadTimestamp() != null ? sharedFile.getUploadTimestamp() : new Timestamp(System.currentTimeMillis()));

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        sharedFile.setId(rs.getLong(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            LoggerUtil.error("Error saving shared file metadata: " + sharedFile.getOriginalName(), e);
        }
        return false;
    }

    @Override
    public SharedFile findById(Long id) {
        String sql = "SELECT * FROM shared_files WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToSharedFile(rs);
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("Error finding shared file by ID " + id, e);
        }
        return null;
    }

    @Override
    public SharedFile findByMessageId(Long messageId) {
        String sql = "SELECT * FROM shared_files WHERE message_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, messageId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToSharedFile(rs);
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("Error finding shared file by message ID " + messageId, e);
        }
        return null;
    }

    @Override
    public SharedFile findByStoredName(String storedName) {
        String sql = "SELECT * FROM shared_files WHERE stored_name = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, storedName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToSharedFile(rs);
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("Error finding shared file by stored name " + storedName, e);
        }
        return null;
    }

    private SharedFile mapRowToSharedFile(ResultSet rs) throws SQLException {
        SharedFile file = new SharedFile();
        file.setId(rs.getLong("id"));

        long msgId = rs.getLong("message_id");
        if (!rs.wasNull()) file.setMessageId(msgId);

        file.setOriginalName(rs.getString("original_name"));
        file.setStoredName(rs.getString("stored_name"));
        file.setFileSizeBytes(rs.getLong("file_size_bytes"));
        file.setFileType(rs.getString("file_type"));
        file.setUploadTimestamp(rs.getTimestamp("upload_timestamp"));
        return file;
    }
}

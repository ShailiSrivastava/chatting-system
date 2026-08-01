package com.chat.server.dao.impl;

import com.chat.common.model.User;
import com.chat.common.model.UserStatus;
import com.chat.common.util.LoggerUtil;
import com.chat.server.dao.UserDAO;
import com.chat.server.db.DatabaseConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAOImpl implements UserDAO {

    private final DatabaseConnectionManager dbManager = DatabaseConnectionManager.getInstance();

    @Override
    public boolean createUser(User user) {
        String sql = "INSERT INTO users (username, email, password_hash, salt, status, bio, avatar_path, last_active_at, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setString(4, user.getSalt());
            pstmt.setString(5, user.getStatus() != null ? user.getStatus().name() : UserStatus.OFFLINE.name());
            pstmt.setString(6, user.getBio() != null ? user.getBio() : "");
            pstmt.setString(7, user.getAvatarPath() != null ? user.getAvatarPath() : "");
            pstmt.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
            pstmt.setTimestamp(9, new Timestamp(System.currentTimeMillis()));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        user.setId(rs.getLong(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            LoggerUtil.error("Error creating user: " + user.getUsername(), e);
        }
        return false;
    }

    @Override
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("Error finding user by username: " + username, e);
        }
        return null;
    }

    @Override
    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("Error finding user by email: " + email, e);
        }
        return null;
    }

    @Override
    public User findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("Error finding user by ID: " + id, e);
        }
        return null;
    }

    @Override
    public List<User> searchUsers(String query) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE LOWER(username) LIKE LOWER(?) OR LOWER(email) LIKE LOWER(?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String searchPattern = "%" + query + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("Error searching users with query: " + query, e);
        }
        return list;
    }

    @Override
    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY username ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            LoggerUtil.error("Error getting all users", e);
        }
        return list;
    }

    @Override
    public boolean updateStatus(Long userId, UserStatus status) {
        String sql = "UPDATE users SET status = ?, last_active_at = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            pstmt.setLong(3, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.error("Error updating status for user ID: " + userId, e);
        }
        return false;
    }

    @Override
    public boolean updateProfile(Long userId, String bio, String avatarPath) {
        String sql = "UPDATE users SET bio = ?, avatar_path = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, bio);
            pstmt.setString(2, avatarPath);
            pstmt.setLong(3, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.error("Error updating profile for user ID: " + userId, e);
        }
        return false;
    }

    @Override
    public boolean updatePassword(Long userId, String newPasswordHash, String newSalt) {
        String sql = "UPDATE users SET password_hash = ?, salt = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newPasswordHash);
            pstmt.setString(2, newSalt);
            pstmt.setLong(3, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.error("Error updating password for user ID: " + userId, e);
        }
        return false;
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setSalt(rs.getString("salt"));
        user.setStatus(UserStatus.valueOf(rs.getString("status")));
        user.setBio(rs.getString("bio"));
        user.setAvatarPath(rs.getString("avatar_path"));
        user.setLastActiveAt(rs.getTimestamp("last_active_at"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        return user;
    }
}

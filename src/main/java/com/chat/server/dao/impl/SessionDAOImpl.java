package com.chat.server.dao.impl;

import com.chat.common.model.UserSession;
import com.chat.common.util.LoggerUtil;
import com.chat.server.dao.SessionDAO;
import com.chat.server.db.DatabaseConnectionManager;

import java.sql.*;

public class SessionDAOImpl implements SessionDAO {

    private final DatabaseConnectionManager dbManager = DatabaseConnectionManager.getInstance();

    @Override
    public boolean createSession(UserSession session) {
        String sql = "INSERT INTO user_sessions (user_id, session_token, login_time, last_seen_time, is_active) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, session.getUserId());
            pstmt.setString(2, session.getSessionToken());
            pstmt.setTimestamp(3, session.getLoginTime() != null ? session.getLoginTime() : new Timestamp(System.currentTimeMillis()));
            pstmt.setTimestamp(4, session.getLastSeenTime() != null ? session.getLastSeenTime() : new Timestamp(System.currentTimeMillis()));
            pstmt.setBoolean(5, session.isActive());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) session.setId(rs.getLong(1));
                }
                return true;
            }
        } catch (SQLException e) {
            LoggerUtil.error("Error creating session for user " + session.getUserId(), e);
        }
        return false;
    }

    @Override
    public UserSession findByToken(String sessionToken) {
        String sql = "SELECT * FROM user_sessions WHERE session_token = ? AND is_active = TRUE";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sessionToken);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    UserSession s = new UserSession();
                    s.setId(rs.getLong("id"));
                    s.setUserId(rs.getLong("user_id"));
                    s.setSessionToken(rs.getString("session_token"));
                    s.setLoginTime(rs.getTimestamp("login_time"));
                    s.setLastSeenTime(rs.getTimestamp("last_seen_time"));
                    s.setActive(rs.getBoolean("is_active"));
                    return s;
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("Error finding session by token", e);
        }
        return null;
    }

    @Override
    public boolean invalidateSession(String sessionToken) {
        String sql = "UPDATE user_sessions SET is_active = FALSE WHERE session_token = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sessionToken);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.error("Error invalidating session", e);
        }
        return false;
    }

    @Override
    public boolean invalidateAllUserSessions(Long userId) {
        String sql = "UPDATE user_sessions SET is_active = FALSE WHERE user_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.error("Error invalidating all sessions for user " + userId, e);
        }
        return false;
    }
}

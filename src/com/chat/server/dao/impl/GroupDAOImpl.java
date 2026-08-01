package com.chat.server.dao.impl;

import com.chat.common.model.ChatGroup;
import com.chat.common.model.Role;
import com.chat.common.model.User;
import com.chat.common.model.UserStatus;
import com.chat.common.util.LoggerUtil;
import com.chat.server.dao.GroupDAO;
import com.chat.server.db.DatabaseConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupDAOImpl implements GroupDAO {

    private final DatabaseConnectionManager dbManager = DatabaseConnectionManager.getInstance();

    @Override
    public boolean createGroup(ChatGroup group, List<Long> memberUserIds) {
        String insertGroupSql = "INSERT INTO chat_groups (name, description, created_by, created_at) VALUES (?, ?, ?, ?)";
        String insertMemberSql = "INSERT INTO group_members (group_id, user_id, role, joined_at) VALUES (?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false); // Transaction

            try (PreparedStatement pstmtGroup = conn.prepareStatement(insertGroupSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmtGroup.setString(1, group.getName());
                pstmtGroup.setString(2, group.getDescription());
                pstmtGroup.setLong(3, group.getCreatedBy());
                pstmtGroup.setTimestamp(4, new Timestamp(System.currentTimeMillis()));

                int affected = pstmtGroup.executeUpdate();
                if (affected == 0) {
                    conn.rollback();
                    return false;
                }

                try (ResultSet rs = pstmtGroup.getGeneratedKeys()) {
                    if (rs.next()) {
                        group.setId(rs.getLong(1));
                    }
                }
            }

            try (PreparedStatement pstmtMember = conn.prepareStatement(insertMemberSql)) {
                // Add Creator as ADMIN
                pstmtMember.setLong(1, group.getId());
                pstmtMember.setLong(2, group.getCreatedBy());
                pstmtMember.setString(3, Role.ADMIN.name());
                pstmtMember.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                pstmtMember.executeUpdate();

                // Add Members as MEMBER
                for (Long uId : memberUserIds) {
                    if (uId.equals(group.getCreatedBy())) continue;
                    pstmtMember.setLong(1, group.getId());
                    pstmtMember.setLong(2, uId);
                    pstmtMember.setString(3, Role.MEMBER.name());
                    pstmtMember.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                    pstmtMember.executeUpdate();
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignore */ }
            }
            LoggerUtil.error("Error creating group: " + group.getName(), e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { /* ignore */ }
            }
        }
        return false;
    }

    @Override
    public ChatGroup findById(Long groupId) {
        String sql = "SELECT * FROM chat_groups WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, groupId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToGroup(rs);
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("Error finding group by ID " + groupId, e);
        }
        return null;
    }

    @Override
    public List<ChatGroup> getUserGroups(Long userId) {
        List<ChatGroup> list = new ArrayList<>();
        String sql = "SELECT g.* FROM chat_groups g " +
                "JOIN group_members gm ON g.id = gm.group_id " +
                "WHERE gm.user_id = ? ORDER BY g.name ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToGroup(rs));
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("Error getting user groups for user " + userId, e);
        }
        return list;
    }

    @Override
    public List<User> getGroupMembers(Long groupId) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT u.* FROM users u " +
                "JOIN group_members gm ON u.id = gm.user_id " +
                "WHERE gm.group_id = ? ORDER BY u.username ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, groupId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    User u = new User();
                    u.setId(rs.getLong("id"));
                    u.setUsername(rs.getString("username"));
                    u.setEmail(rs.getString("email"));
                    u.setStatus(UserStatus.valueOf(rs.getString("status")));
                    list.add(u);
                }
            }
        } catch (SQLException e) {
            LoggerUtil.error("Error getting members for group " + groupId, e);
        }
        return list;
    }

    @Override
    public boolean addMember(Long groupId, Long userId, Role role) {
        String sql = "INSERT INTO group_members (group_id, user_id, role, joined_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, groupId);
            pstmt.setLong(2, userId);
            pstmt.setString(3, role != null ? role.name() : Role.MEMBER.name());
            pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.error("Error adding user " + userId + " to group " + groupId, e);
        }
        return false;
    }

    @Override
    public boolean removeMember(Long groupId, Long userId) {
        String sql = "DELETE FROM group_members WHERE group_id = ? AND user_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, groupId);
            pstmt.setLong(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LoggerUtil.error("Error removing user " + userId + " from group " + groupId, e);
        }
        return false;
    }

    private ChatGroup mapRowToGroup(ResultSet rs) throws SQLException {
        ChatGroup group = new ChatGroup();
        group.setId(rs.getLong("id"));
        group.setName(rs.getString("name"));
        group.setDescription(rs.getString("description"));
        group.setCreatedBy(rs.getLong("created_by"));
        group.setCreatedAt(rs.getTimestamp("created_at"));
        return group;
    }
}

package com.chat.common.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class GroupMember implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long groupId;
    private Long userId;
    private Role role;
    private Timestamp joinedAt;

    public GroupMember() {}

    public GroupMember(Long groupId, Long userId, Role role) {
        this.groupId = groupId;
        this.userId = userId;
        this.role = role;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Timestamp getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Timestamp joinedAt) { this.joinedAt = joinedAt; }
}

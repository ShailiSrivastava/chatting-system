package com.chat.server.service;

import com.chat.common.model.ChatGroup;
import com.chat.common.model.Role;
import com.chat.common.model.User;
import com.chat.server.dao.GroupDAO;
import com.chat.server.dao.impl.GroupDAOImpl;

import java.util.List;

public class GroupService {

    private final GroupDAO groupDAO;

    public GroupService() {
        this.groupDAO = new GroupDAOImpl();
    }

    public ChatGroup createGroup(String groupName, String description, Long creatorId, List<Long> memberIds) throws Exception {
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be empty.");
        }

        ChatGroup group = new ChatGroup();
        group.setName(groupName.trim());
        group.setDescription(description != null ? description.trim() : "");
        group.setCreatedBy(creatorId);

        boolean created = groupDAO.createGroup(group, memberIds);
        if (created) {
            return group;
        }
        throw new RuntimeException("Failed to create group.");
    }

    public List<ChatGroup> getUserGroups(Long userId) {
        return groupDAO.getUserGroups(userId);
    }

    public List<User> getGroupMembers(Long groupId) {
        return groupDAO.getGroupMembers(groupId);
    }

    public boolean addMember(Long groupId, Long userId, Role role) {
        return groupDAO.addMember(groupId, userId, role);
    }

    public boolean removeMember(Long groupId, Long userId) {
        return groupDAO.removeMember(groupId, userId);
    }
}

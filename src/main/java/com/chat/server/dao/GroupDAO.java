package com.chat.server.dao;

import com.chat.common.model.ChatGroup;
import com.chat.common.model.GroupMember;
import com.chat.common.model.Role;
import com.chat.common.model.User;

import java.util.List;

public interface GroupDAO {
    boolean createGroup(ChatGroup group, List<Long> memberUserIds);
    ChatGroup findById(Long groupId);
    List<ChatGroup> getUserGroups(Long userId);
    List<User> getGroupMembers(Long groupId);
    boolean addMember(Long groupId, Long userId, Role role);
    boolean removeMember(Long groupId, Long userId);
}

package com.chat.client.listener;

import com.chat.common.model.ChatGroup;
import com.chat.common.model.Message;
import com.chat.common.model.SharedFile;
import com.chat.common.model.User;

import java.util.List;

public interface ServerEventListener {
    void onLoginSuccess(User user, String token);
    void onLoginFailure(String error);
    void onRegisterSuccess(User user);
    void onRegisterFailure(String error);
    void onUserListReceived(List<User> users);
    void onUserSearchReceived(List<User> users);
    void onMessageReceived(Message message);
    void onMessageDelivered(Message message);
    void onMessageRead(Long senderId);
    void onTypingIndicator(Long senderId, String senderUsername, Long groupId, boolean isTyping);
    void onChatHistoryReceived(List<Message> history);
    void onGroupCreated(ChatGroup group);
    void onUserGroupsReceived(List<ChatGroup> groups);
    void onFileUploadResponse(Message message, SharedFile sharedFile);
    void onFileDownloadResponse(String storedName, byte[] fileData);
    void onUserStatusChanged(Long userId, String status);
    void onErrorReceived(String error);
}

package com.chat.server.dao;

import com.chat.common.model.Message;
import com.chat.common.model.MessageStatus;

import java.util.List;

public interface MessageDAO {
    boolean saveMessage(Message message);
    List<Message> getPrivateChatHistory(Long userId1, Long userId2);
    List<Message> getGroupChatHistory(Long groupId);
    List<Message> getPendingOfflineMessages(Long receiverId);
    boolean updateMessageStatus(Long messageId, MessageStatus status);
    boolean markMessagesAsDelivered(Long receiverId);
    boolean markMessagesAsRead(Long senderId, Long receiverId);
    List<Message> searchChatHistory(Long userId, String keyword);
}

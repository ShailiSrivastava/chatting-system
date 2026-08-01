package com.chat.server.service;

import com.chat.common.model.Message;
import com.chat.common.model.MessageStatus;
import com.chat.common.util.AESEncryptionUtil;
import com.chat.server.dao.MessageDAO;
import com.chat.server.dao.impl.MessageDAOImpl;

import java.util.List;

public class MessageService {

    private final MessageDAO messageDAO;

    public MessageService() {
        this.messageDAO = new MessageDAOImpl();
    }

    public Message sendMessage(Message message) {
        if (message.isEncrypted()) {
            String encryptedContent = AESEncryptionUtil.encrypt(message.getContent());
            message.setContent(encryptedContent);
        }

        boolean saved = messageDAO.saveMessage(message);
        if (saved) {
            // Decrypt content for memory object if encrypted so recipient gets plaintext
            if (message.isEncrypted()) {
                message.setContent(AESEncryptionUtil.decrypt(message.getContent()));
            }
            return message;
        }
        return null;
    }

    public List<Message> getPrivateChatHistory(Long userId1, Long userId2) {
        List<Message> history = messageDAO.getPrivateChatHistory(userId1, userId2);
        decryptMessageList(history);
        return history;
    }

    public List<Message> getGroupChatHistory(Long groupId) {
        List<Message> history = messageDAO.getGroupChatHistory(groupId);
        decryptMessageList(history);
        return history;
    }

    public List<Message> getPendingOfflineMessages(Long receiverId) {
        List<Message> pending = messageDAO.getPendingOfflineMessages(receiverId);
        decryptMessageList(pending);
        return pending;
    }

    public boolean markMessagesAsDelivered(Long receiverId) {
        return messageDAO.markMessagesAsDelivered(receiverId);
    }

    public boolean markMessagesAsRead(Long senderId, Long receiverId) {
        return messageDAO.markMessagesAsRead(senderId, receiverId);
    }

    public boolean updateStatus(Long messageId, MessageStatus status) {
        return messageDAO.updateMessageStatus(messageId, status);
    }

    public List<Message> searchMessages(Long userId, String keyword) {
        List<Message> results = messageDAO.searchChatHistory(userId, keyword);
        decryptMessageList(results);
        return results;
    }

    private void decryptMessageList(List<Message> messages) {
        for (Message m : messages) {
            if (m.isEncrypted()) {
                m.setContent(AESEncryptionUtil.decrypt(m.getContent()));
            }
        }
    }
}

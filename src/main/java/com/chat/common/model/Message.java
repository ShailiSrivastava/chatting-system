package com.chat.common.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long senderId;
    private String senderUsername;
    private Long receiverId;
    private Long groupId;
    private String content;
    private MessageType messageType;
    private MessageStatus status;
    private boolean isEncrypted;
    private Timestamp timestamp;

    public Message() {
        this.messageType = MessageType.TEXT;
        this.status = MessageStatus.SENT;
        this.isEncrypted = false;
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }

    public Message(Long senderId, String senderUsername, Long receiverId, Long groupId, String content, MessageType messageType) {
        this();
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.receiverId = receiverId;
        this.groupId = groupId;
        this.content = content;
        this.messageType = messageType;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }

    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }

    public boolean isEncrypted() { return isEncrypted; }
    public void setEncrypted(boolean encrypted) { isEncrypted = encrypted; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public boolean isGroupMessage() {
        return groupId != null && groupId > 0;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", senderId=" + senderId +
                ", receiverId=" + receiverId +
                ", groupId=" + groupId +
                ", messageType=" + messageType +
                ", status=" + status +
                ", timestamp=" + timestamp +
                '}';
    }
}

package com.chat.common.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class SharedFile implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long messageId;
    private String originalName;
    private String storedName;
    private long fileSizeBytes;
    private String fileType;
    private Timestamp uploadTimestamp;
    private byte[] fileData; // optional transient/payload holder for socket transfers

    public SharedFile() {}

    public SharedFile(String originalName, String storedName, long fileSizeBytes, String fileType) {
        this.originalName = originalName;
        this.storedName = storedName;
        this.fileSizeBytes = fileSizeBytes;
        this.fileType = fileType;
        this.uploadTimestamp = new Timestamp(System.currentTimeMillis());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getStoredName() { return storedName; }
    public void setStoredName(String storedName) { this.storedName = storedName; }

    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Timestamp getUploadTimestamp() { return uploadTimestamp; }
    public void setUploadTimestamp(Timestamp uploadTimestamp) { this.uploadTimestamp = uploadTimestamp; }

    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }
}

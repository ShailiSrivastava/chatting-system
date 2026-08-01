package com.chat.server.dao;

import com.chat.common.model.SharedFile;

public interface FileDAO {
    boolean saveFileMetadata(SharedFile sharedFile);
    SharedFile findById(Long id);
    SharedFile findByMessageId(Long messageId);
    SharedFile findByStoredName(String storedName);
}

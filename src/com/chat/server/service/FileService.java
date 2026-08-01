package com.chat.server.service;

import com.chat.common.model.SharedFile;
import com.chat.common.util.FileUtil;
import com.chat.common.util.LoggerUtil;
import com.chat.server.config.ServerConfig;
import com.chat.server.dao.FileDAO;
import com.chat.server.dao.impl.FileDAOImpl;

import java.io.IOException;

public class FileService {

    private final FileDAO fileDAO;

    public FileService() {
        this.fileDAO = new FileDAOImpl();
    }

    public SharedFile saveFile(String originalName, String fileType, byte[] fileData, Long messageId) throws IOException {
        String storageDir = ServerConfig.getInstance().getStorageDirectory();
        String storedName = FileUtil.saveFile(storageDir, originalName, fileData);

        SharedFile sharedFile = new SharedFile(originalName, storedName, fileData.length, fileType);
        sharedFile.setMessageId(messageId);

        boolean saved = fileDAO.saveFileMetadata(sharedFile);
        if (saved) {
            LoggerUtil.info("File saved successfully: " + originalName + " -> " + storedName);
            return sharedFile;
        }
        throw new IOException("Failed to save file metadata to database.");
    }

    public byte[] downloadFile(String storedName) throws IOException {
        String storageDir = ServerConfig.getInstance().getStorageDirectory();
        return FileUtil.readFile(storageDir, storedName);
    }

    public SharedFile getFileMetadata(Long messageId) {
        return fileDAO.findByMessageId(messageId);
    }
}

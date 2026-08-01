package com.chat.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class FileUtil {

    public static String saveFile(String directory, String originalName, byte[] fileData) throws IOException {
        File dir = new File(directory);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String extension = "";
        int i = originalName.lastIndexOf('.');
        if (i > 0) {
            extension = originalName.substring(i);
        }

        String storedName = UUID.randomUUID().toString() + extension;
        File targetFile = new File(dir, storedName);

        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            fos.write(fileData);
            fos.flush();
        }

        return storedName;
    }

    public static byte[] readFile(String directory, String storedName) throws IOException {
        File file = new File(directory, storedName);
        if (!file.exists()) {
            throw new IOException("File not found: " + storedName);
        }

        byte[] data = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(data);
        }
        return data;
    }
}

package com.chat.common.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;

public class AESEncryptionUtil {

    private static final String ALGORITHM = "AES";

    private static Key generateKey() {
        byte[] keyBytes = new byte[16];
        byte[] secretBytes = Constants.AES_SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(secretBytes, 0, keyBytes, 0, Math.min(secretBytes.length, keyBytes.length));
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    public static String encrypt(String valueToEnc) {
        if (valueToEnc == null) return null;
        try {
            Key key = generateKey();
            Cipher c = Cipher.getInstance(ALGORITHM);
            c.init(Cipher.ENCRYPT_MODE, key);
            byte[] encValue = c.doFinal(valueToEnc.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encValue);
        } catch (Exception e) {
            LoggerUtil.error("Encryption error", e);
            return valueToEnc;
        }
    }

    public static String decrypt(String encryptedValue) {
        if (encryptedValue == null) return null;
        try {
            Key key = generateKey();
            Cipher c = Cipher.getInstance(ALGORITHM);
            c.init(Cipher.DECRYPT_MODE, key);
            byte[] decodedValue = Base64.getDecoder().decode(encryptedValue);
            byte[] decValue = c.doFinal(decodedValue);
            return new String(decValue, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LoggerUtil.error("Decryption error", e);
            return encryptedValue;
        }
    }
}

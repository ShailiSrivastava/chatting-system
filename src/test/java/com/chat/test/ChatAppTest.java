package com.chat.test;

import com.chat.common.model.User;
import com.chat.common.model.UserStatus;
import com.chat.common.protocol.Packet;
import com.chat.common.protocol.PacketFactory;
import com.chat.common.protocol.PacketType;
import com.chat.common.util.AESEncryptionUtil;
import com.chat.common.util.PasswordHasher;
import com.chat.common.util.ValidationUtil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

public class ChatAppTest {

    @Test
    @DisplayName("Password Hashing & Salt Verification Test")
    void testPasswordHashing() {
        String salt = PasswordHasher.generateSalt();
        assertNotNull(salt, "Salt should not be null");
        assertFalse(salt.isEmpty(), "Salt should not be empty");

        String password = "SecretPassword123!";
        String hash1 = PasswordHasher.hashPassword(password, salt);
        String hash2 = PasswordHasher.hashPassword(password, salt);

        assertEquals(hash1, hash2, "Deterministic password hashing with same salt");
        assertTrue(PasswordHasher.verifyPassword(password, salt, hash1), "Password verification with correct credentials");
        assertFalse(PasswordHasher.verifyPassword("WrongPassword", salt, hash1), "Password verification failure with wrong credentials");
    }

    @Test
    @DisplayName("AES Symmetric Payload Encryption Test")
    void testAESEncryption() {
        String originalText = "Hello Antigravity Enterprise Chat World!";
        String encrypted = AESEncryptionUtil.encrypt(originalText);

        assertNotNull(encrypted, "Encrypted output should not be null");
        assertNotEquals(originalText, encrypted, "Encrypted output should differ from plaintext");

        String decrypted = AESEncryptionUtil.decrypt(encrypted);
        assertEquals(originalText, decrypted, "AES Encryption/Decryption symmetry");
    }

    @Test
    @DisplayName("Validation Utility Rules Test")
    void testValidationUtils() {
        assertTrue(ValidationUtil.isValidUsername("john_doe"), "Valid username format");
        assertFalse(ValidationUtil.isValidUsername("a"), "Too short username rejected");
        assertTrue(ValidationUtil.isValidEmail("alex@enterprise.com"), "Valid email address format");
        assertFalse(ValidationUtil.isValidEmail("invalid-email"), "Invalid email address rejected");
        assertTrue(ValidationUtil.isValidPassword("pass123"), "Valid password length");
    }

    @Test
    @DisplayName("Object Packet Serialization & Deserialization Test")
    void testPacketSerialization() throws Exception {
        User testUser = new User(100L, "alice", "alice@chat.com", UserStatus.ONLINE);
        Packet originalPacket = PacketFactory.createSuccessPacket(PacketType.LOGIN_RESPONSE, "session-token-999", testUser);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(originalPacket);
        oos.flush();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Packet deserializedPacket = (Packet) ois.readObject();

        assertEquals(originalPacket.getType(), deserializedPacket.getType(), "Packet Type serialization match");
        assertEquals(originalPacket.getSessionToken(), deserializedPacket.getSessionToken(), "Session Token serialization match");
        assertTrue(deserializedPacket.getPayload() instanceof User, "Payload instance type integrity");

        User deserializedUser = (User) deserializedPacket.getPayload();
        assertEquals(testUser.getUsername(), deserializedUser.getUsername(), "Payload contents integrity");
    }
}

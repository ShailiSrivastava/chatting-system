package com.chat.test;

import com.chat.common.model.User;
import com.chat.common.model.UserStatus;
import com.chat.common.protocol.Packet;
import com.chat.common.protocol.PacketFactory;
import com.chat.common.protocol.PacketType;
import com.chat.common.util.AESEncryptionUtil;
import com.chat.common.util.PasswordHasher;
import com.chat.common.util.ValidationUtil;

import java.io.*;

public class SimpleUnitTestRunner {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   ANTIGRAVITY CHAT APPLICATION UNIT TESTS        ");
        System.out.println("==================================================");

        testPasswordHashing();
        testAESEncryption();
        testValidationUtils();
        testPacketSerialization();

        System.out.println("\n--------------------------------------------------");
        System.out.printf("RESULTS: %d PASSED, %d FAILED%n", passed, failed);
        System.out.println("--------------------------------------------------");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void assertEquals(Object expected, Object actual, String testName) {
        if ((expected == null && actual == null) || (expected != null && expected.equals(actual))) {
            System.out.println("[PASS] " + testName);
            passed++;
        } else {
            System.err.println("[FAIL] " + testName + " - Expected: " + expected + ", Got: " + actual);
            failed++;
        }
    }

    private static void assertTrue(boolean condition, String testName) {
        if (condition) {
            System.out.println("[PASS] " + testName);
            passed++;
        } else {
            System.err.println("[FAIL] " + testName + " - Condition was false");
            failed++;
        }
    }

    private static void testPasswordHashing() {
        System.out.println("\n--- Testing Password Hashing & Salt ---");
        String salt = PasswordHasher.generateSalt();
        assertTrue(salt != null && !salt.isEmpty(), "Salt generation");

        String password = "SecretPassword123!";
        String hash1 = PasswordHasher.hashPassword(password, salt);
        String hash2 = PasswordHasher.hashPassword(password, salt);

        assertEquals(hash1, hash2, "Deterministic password hashing with same salt");
        assertTrue(PasswordHasher.verifyPassword(password, salt, hash1), "Password verification with correct credentials");
        assertTrue(!PasswordHasher.verifyPassword("WrongPassword", salt, hash1), "Password verification failure with wrong credentials");
    }

    private static void testAESEncryption() {
        System.out.println("\n--- Testing AES Symmetric Payload Encryption ---");
        String originalText = "Hello Antigravity Enterprise Chat World!";
        String encrypted = AESEncryptionUtil.encrypt(originalText);

        assertTrue(encrypted != null && !encrypted.equals(originalText), "Text encryption succeeds and changes output");
        String decrypted = AESEncryptionUtil.decrypt(encrypted);
        assertEquals(originalText, decrypted, "AES Encryption/Decryption symmetry");
    }

    private static void testValidationUtils() {
        System.out.println("\n--- Testing Validation Rules ---");
        assertTrue(ValidationUtil.isValidUsername("john_doe"), "Valid username format");
        assertTrue(!ValidationUtil.isValidUsername("a"), "Too short username rejected");
        assertTrue(ValidationUtil.isValidEmail("alex@enterprise.com"), "Valid email address format");
        assertTrue(!ValidationUtil.isValidEmail("invalid-email"), "Invalid email address rejected");
        assertTrue(ValidationUtil.isValidPassword("pass123"), "Valid password length");
    }

    private static void testPacketSerialization() {
        System.out.println("\n--- Testing Object Packet Serialization ---");
        User testUser = new User(100L, "alice", "alice@chat.com", UserStatus.ONLINE);
        Packet originalPacket = PacketFactory.createSuccessPacket(PacketType.LOGIN_RESPONSE, "session-token-999", testUser);

        try {
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

        } catch (Exception e) {
            assertTrue(false, "Packet serialization exception: " + e.getMessage());
        }
    }
}

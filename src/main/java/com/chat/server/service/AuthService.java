package com.chat.server.service;

import com.chat.common.model.User;
import com.chat.common.model.UserSession;
import com.chat.common.model.UserStatus;
import com.chat.common.util.LoggerUtil;
import com.chat.common.util.PasswordHasher;
import com.chat.common.util.ValidationUtil;
import com.chat.server.dao.SessionDAO;
import com.chat.server.dao.UserDAO;
import com.chat.server.dao.impl.SessionDAOImpl;
import com.chat.server.dao.impl.UserDAOImpl;

import java.util.UUID;

public class AuthService {

    private final UserDAO userDAO;
    private final SessionDAO sessionDAO;

    public AuthService() {
        this.userDAO = new UserDAOImpl();
        this.sessionDAO = new SessionDAOImpl();
    }

    public synchronized User register(String username, String email, String password) throws Exception {
        username = ValidationUtil.sanitize(username);
        email = ValidationUtil.sanitize(email);

        if (!ValidationUtil.isValidUsername(username)) {
            throw new IllegalArgumentException("Username must be 3-20 alphanumeric characters.");
        }
        if (!ValidationUtil.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email address format.");
        }
        if (!ValidationUtil.isValidPassword(password)) {
            throw new IllegalArgumentException("Password must be at least 4 characters long.");
        }

        if (userDAO.findByUsername(username) != null) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken.");
        }
        if (userDAO.findByEmail(email) != null) {
            throw new IllegalArgumentException("Email '" + email + "' is already registered.");
        }

        String salt = PasswordHasher.generateSalt();
        String passwordHash = PasswordHasher.hashPassword(password, salt);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setSalt(salt);
        user.setStatus(UserStatus.OFFLINE);
        user.setBio("Hello! I am using Antigravity Chat.");
        user.setAvatarPath("");

        boolean created = userDAO.createUser(user);
        if (!created) {
            throw new RuntimeException("Failed to register user due to a database error.");
        }

        LoggerUtil.info("User registered successfully: " + username);
        return user;
    }

    public synchronized String login(String usernameOrEmail, String password) throws Exception {
        usernameOrEmail = ValidationUtil.sanitize(usernameOrEmail);

        User user = userDAO.findByUsername(usernameOrEmail);
        if (user == null) {
            user = userDAO.findByEmail(usernameOrEmail);
        }

        if (user == null) {
            throw new IllegalArgumentException("Invalid username or password.");
        }

        boolean valid = PasswordHasher.verifyPassword(password, user.getSalt(), user.getPasswordHash());
        if (!valid) {
            throw new IllegalArgumentException("Invalid username or password.");
        }

        // Invalidate old sessions
        sessionDAO.invalidateAllUserSessions(user.getId());

        // Update User status
        userDAO.updateStatus(user.getId(), UserStatus.ONLINE);

        // Generate Session Token
        String token = UUID.randomUUID().toString();
        UserSession session = new UserSession(user.getId(), token);
        sessionDAO.createSession(session);

        LoggerUtil.info("User logged in successfully: " + user.getUsername() + " [Token: " + token + "]");
        return token;
    }

    public User validateSession(String sessionToken) {
        if (sessionToken == null || sessionToken.isEmpty()) return null;
        UserSession session = sessionDAO.findByToken(sessionToken);
        if (session != null && session.isActive()) {
            return userDAO.findById(session.getUserId());
        }
        return null;
    }

    public synchronized void logout(String sessionToken) {
        if (sessionToken != null) {
            UserSession session = sessionDAO.findByToken(sessionToken);
            if (session != null) {
                userDAO.updateStatus(session.getUserId(), UserStatus.OFFLINE);
                sessionDAO.invalidateSession(sessionToken);
                LoggerUtil.info("Session logged out successfully: " + sessionToken);
            }
        }
    }
}

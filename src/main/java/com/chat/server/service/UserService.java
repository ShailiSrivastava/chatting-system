package com.chat.server.service;

import com.chat.common.model.User;
import com.chat.common.model.UserStatus;
import com.chat.common.util.PasswordHasher;
import com.chat.common.util.ValidationUtil;
import com.chat.server.dao.UserDAO;
import com.chat.server.dao.impl.UserDAOImpl;

import java.util.List;

public class UserService {

    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAOImpl();
    }

    public List<User> searchUsers(String query) {
        return userDAO.searchUsers(query);
    }

    public List<User> getAllUsers() {
        return userDAO.getAllUsers();
    }

    public User getUserById(Long id) {
        return userDAO.findById(id);
    }

    public boolean updateStatus(Long userId, UserStatus status) {
        return userDAO.updateStatus(userId, status);
    }

    public boolean updateProfile(Long userId, String bio, String avatarPath) {
        return userDAO.updateProfile(userId, bio, avatarPath);
    }

    public boolean changePassword(Long userId, String currentPassword, String newPassword) throws Exception {
        User user = userDAO.findById(userId);
        if (user == null) throw new IllegalArgumentException("User not found.");

        boolean matches = PasswordHasher.verifyPassword(currentPassword, user.getSalt(), user.getPasswordHash());
        if (!matches) {
            throw new IllegalArgumentException("Current password does not match.");
        }

        if (!ValidationUtil.isValidPassword(newPassword)) {
            throw new IllegalArgumentException("New password must be at least 4 characters long.");
        }

        String newSalt = PasswordHasher.generateSalt();
        String newHash = PasswordHasher.hashPassword(newPassword, newSalt);

        return userDAO.updatePassword(userId, newHash, newSalt);
    }
}

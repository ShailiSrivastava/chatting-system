package com.chat.server.dao;

import com.chat.common.model.User;
import com.chat.common.model.UserStatus;

import java.util.List;

public interface UserDAO {
    boolean createUser(User user);
    User findByUsername(String username);
    User findByEmail(String email);
    User findById(Long id);
    List<User> searchUsers(String query);
    List<User> getAllUsers();
    boolean updateStatus(Long userId, UserStatus status);
    boolean updateProfile(Long userId, String bio, String avatarPath);
    boolean updatePassword(Long userId, String newPasswordHash, String newSalt);
}

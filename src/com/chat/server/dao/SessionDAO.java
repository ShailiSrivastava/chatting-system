package com.chat.server.dao;

import com.chat.common.model.UserSession;

public interface SessionDAO {
    boolean createSession(UserSession session);
    UserSession findByToken(String sessionToken);
    boolean invalidateSession(String sessionToken);
    boolean invalidateAllUserSessions(Long userId);
}

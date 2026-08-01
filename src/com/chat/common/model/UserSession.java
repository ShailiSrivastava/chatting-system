package com.chat.common.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class UserSession implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String sessionToken;
    private Timestamp loginTime;
    private Timestamp lastSeenTime;
    private boolean isActive;

    public UserSession() {
        this.isActive = true;
        this.loginTime = new Timestamp(System.currentTimeMillis());
        this.lastSeenTime = new Timestamp(System.currentTimeMillis());
    }

    public UserSession(Long userId, String sessionToken) {
        this();
        this.userId = userId;
        this.sessionToken = sessionToken;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }

    public Timestamp getLoginTime() { return loginTime; }
    public void setLoginTime(Timestamp loginTime) { this.loginTime = loginTime; }

    public Timestamp getLastSeenTime() { return lastSeenTime; }
    public void setLastSeenTime(Timestamp lastSeenTime) { this.lastSeenTime = lastSeenTime; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}

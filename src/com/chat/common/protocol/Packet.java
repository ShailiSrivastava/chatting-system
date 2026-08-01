package com.chat.common.protocol;

import java.io.Serializable;
import java.sql.Timestamp;

public class Packet implements Serializable {
    private static final long serialVersionUID = 1L;

    private PacketType type;
    private String sessionToken;
    private boolean success;
    private String message;
    private Object payload;
    private Timestamp timestamp;

    public Packet() {
        this.timestamp = new Timestamp(System.currentTimeMillis());
        this.success = true;
    }

    public Packet(PacketType type, Object payload) {
        this();
        this.type = type;
        this.payload = payload;
    }

    public Packet(PacketType type, String sessionToken, Object payload) {
        this(type, payload);
        this.sessionToken = sessionToken;
    }

    public PacketType getType() { return type; }
    public void setType(PacketType type) { this.type = type; }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "Packet{" +
                "type=" + type +
                ", success=" + success +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

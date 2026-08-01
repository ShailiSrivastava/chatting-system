package com.chat.server.network;

import com.chat.common.model.UserStatus;
import com.chat.common.protocol.Packet;
import com.chat.common.protocol.PacketFactory;
import com.chat.common.protocol.PacketType;
import com.chat.common.util.LoggerUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientManager {

    private static ClientManager instance;

    // Maps userId -> active ClientHandler
    private final ConcurrentHashMap<Long, ClientHandler> activeClients = new ConcurrentHashMap<>();

    private ClientManager() {}

    public static synchronized ClientManager getInstance() {
        if (instance == null) {
            instance = new ClientManager();
        }
        return instance;
    }

    public void registerClient(Long userId, ClientHandler handler) {
        activeClients.put(userId, handler);
        LoggerUtil.info("Client registered in ClientManager: User ID " + userId + " (Active online count: " + activeClients.size() + ")");
    }

    public void unregisterClient(Long userId) {
        if (userId != null) {
            activeClients.remove(userId);
            LoggerUtil.info("Client unregistered from ClientManager: User ID " + userId + " (Active online count: " + activeClients.size() + ")");
        }
    }

    public ClientHandler getClientHandler(Long userId) {
        return activeClients.get(userId);
    }

    public boolean isUserOnline(Long userId) {
        return activeClients.containsKey(userId);
    }

    public boolean sendDirectPacket(Long receiverId, Packet packet) {
        ClientHandler handler = activeClients.get(receiverId);
        if (handler != null) {
            handler.sendPacket(packet);
            return true;
        }
        return false;
    }

    public void broadcastGroupPacket(List<Long> memberIds, Packet packet, Long excludeSenderId) {
        for (Long memberId : memberIds) {
            if (excludeSenderId != null && excludeSenderId.equals(memberId)) {
                continue;
            }
            sendDirectPacket(memberId, packet);
        }
    }

    public void broadcastStatusChange(Long userId, UserStatus status) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("status", status.name());

        Packet packet = PacketFactory.createSuccessPacket(PacketType.USER_STATUS_CHANGE, payload);
        for (Map.Entry<Long, ClientHandler> entry : activeClients.entrySet()) {
            if (!entry.getKey().equals(userId)) {
                entry.getValue().sendPacket(packet);
            }
        }
    }

    public int getActiveCount() {
        return activeClients.size();
    }
}

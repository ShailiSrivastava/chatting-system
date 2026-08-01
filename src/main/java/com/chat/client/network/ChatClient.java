package com.chat.client.network;

import com.chat.client.listener.ServerEventListener;
import com.chat.common.model.ChatGroup;
import com.chat.common.model.Message;
import com.chat.common.model.SharedFile;
import com.chat.common.model.User;
import com.chat.common.protocol.Packet;
import com.chat.common.protocol.PacketFactory;
import com.chat.common.protocol.PacketType;
import com.chat.common.util.Constants;
import com.chat.common.util.LoggerUtil;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatClient {

    private String host;
    private int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private String sessionToken;
    private User currentUser;
    private final List<ServerEventListener> listeners = new ArrayList<>();
    private volatile boolean connected = false;

    public ChatClient() {
        this(Constants.DEFAULT_SERVER_HOST, Constants.DEFAULT_SERVER_PORT);
    }

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;

            // Start background listening thread
            Thread readerThread = new Thread(this::listenLoop, "ClientPacketReader");
            readerThread.setDaemon(true);
            readerThread.start();

            LoggerUtil.info("Connected to Chat Server at " + host + ":" + port);
            return true;
        } catch (IOException e) {
            LoggerUtil.error("Failed to connect to Chat Server at " + host + ":" + port, e);
            return false;
        }
    }

    public void addListener(ServerEventListener listener) {
        synchronized (listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    public void removeListener(ServerEventListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public void sendPacket(Packet packet) {
        if (!connected || socket.isClosed()) {
            notifyError("Not connected to server.");
            return;
        }
        try {
            if (sessionToken != null) {
                packet.setSessionToken(sessionToken);
            }
            synchronized (out) {
                out.writeObject(packet);
                out.flush();
            }
        } catch (IOException e) {
            LoggerUtil.error("Error sending packet to server", e);
            notifyError("Connection error: " + e.getMessage());
        }
    }

    // High-level API methods
    public void register(String username, String email, String password) {
        Map<String, String> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("email", email);
        payload.put("password", password);
        sendPacket(PacketFactory.createSuccessPacket(PacketType.REGISTER_REQUEST, payload));
    }

    public void login(String usernameOrEmail, String password) {
        Map<String, String> payload = new HashMap<>();
        payload.put("usernameOrEmail", usernameOrEmail);
        payload.put("password", password);
        sendPacket(PacketFactory.createSuccessPacket(PacketType.LOGIN_REQUEST, payload));
    }

    public void logout() {
        sendPacket(PacketFactory.createSuccessPacket(PacketType.LOGOUT_REQUEST, null));
        disconnect();
    }

    public void requestUserList() {
        sendPacket(PacketFactory.createSuccessPacket(PacketType.GET_USER_LIST_REQUEST, null));
    }

    public void searchUser(String query) {
        sendPacket(PacketFactory.createSuccessPacket(PacketType.SEARCH_USER_REQUEST, query));
    }

    public void sendMessage(Message message) {
        sendPacket(PacketFactory.createSuccessPacket(PacketType.CHAT_MESSAGE, message));
    }

    public void sendTypingIndicator(Long receiverId, Long groupId, boolean isTyping) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("receiverId", receiverId);
        payload.put("groupId", groupId);
        payload.put("isTyping", isTyping);
        sendPacket(PacketFactory.createSuccessPacket(PacketType.TYPING_INDICATOR, payload));
    }

    public void markMessagesAsRead(Long senderId) {
        Map<String, Long> payload = new HashMap<>();
        payload.put("senderId", senderId);
        sendPacket(PacketFactory.createSuccessPacket(PacketType.MESSAGE_READ, payload));
    }

    public void requestChatHistory(Long otherUserId, Long groupId) {
        Map<String, Long> payload = new HashMap<>();
        payload.put("otherUserId", otherUserId);
        payload.put("groupId", groupId);
        sendPacket(PacketFactory.createSuccessPacket(PacketType.GET_CHAT_HISTORY_REQUEST, payload));
    }

    public void createGroup(String groupName, String description, List<Long> memberIds) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", groupName);
        payload.put("description", description);
        payload.put("memberIds", memberIds);
        sendPacket(PacketFactory.createSuccessPacket(PacketType.CREATE_GROUP_REQUEST, payload));
    }

    public void requestUserGroups() {
        sendPacket(PacketFactory.createSuccessPacket(PacketType.GET_USER_GROUPS_REQUEST, null));
    }

    public void uploadFile(String fileName, String fileType, byte[] fileData, Long receiverId, Long groupId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("fileName", fileName);
        payload.put("fileType", fileType);
        payload.put("fileData", fileData);
        payload.put("receiverId", receiverId);
        payload.put("groupId", groupId);
        sendPacket(PacketFactory.createSuccessPacket(PacketType.FILE_UPLOAD_REQUEST, payload));
    }

    public void downloadFile(String storedName) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("storedName", storedName);
        sendPacket(PacketFactory.createSuccessPacket(PacketType.FILE_DOWNLOAD_REQUEST, payload));
    }

    public void updateProfile(String bio, String avatarPath, String currentPassword, String newPassword) {
        Map<String, String> payload = new HashMap<>();
        if (bio != null) payload.put("bio", bio);
        if (avatarPath != null) payload.put("avatarPath", avatarPath);
        if (currentPassword != null) payload.put("currentPassword", currentPassword);
        if (newPassword != null) payload.put("newPassword", newPassword);
        sendPacket(PacketFactory.createSuccessPacket(PacketType.UPDATE_PROFILE_REQUEST, payload));
    }

    private void listenLoop() {
        try {
            while (connected && !socket.isClosed()) {
                Object obj = in.readObject();
                if (obj instanceof Packet) {
                    Packet packet = (Packet) obj;
                    dispatchPacketToListeners(packet);
                }
            }
        } catch (Exception e) {
            if (connected) {
                LoggerUtil.info("Disconnected from server.");
                notifyError("Connection to server lost.");
            }
        } finally {
            disconnect();
        }
    }

    private void dispatchPacketToListeners(Packet packet) {
        SwingUtilities.invokeLater(() -> {
            synchronized (listeners) {
                for (ServerEventListener listener : listeners) {
                    try {
                        if (!packet.isSuccess()) {
                            listener.onErrorReceived(packet.getMessage());
                            continue;
                        }

                        switch (packet.getType()) {
                            case LOGIN_RESPONSE:
                                Map<String, Object> loginData = (Map<String, Object>) packet.getPayload();
                                currentUser = (User) loginData.get("user");
                                sessionToken = (String) loginData.get("token");
                                listener.onLoginSuccess(currentUser, sessionToken);
                                break;
                            case REGISTER_RESPONSE:
                                listener.onRegisterSuccess((User) packet.getPayload());
                                break;
                            case GET_USER_LIST_RESPONSE:
                                listener.onUserListReceived((List<User>) packet.getPayload());
                                break;
                            case SEARCH_USER_RESPONSE:
                                listener.onUserSearchReceived((List<User>) packet.getPayload());
                                break;
                            case CHAT_MESSAGE:
                                listener.onMessageReceived((Message) packet.getPayload());
                                break;
                            case MESSAGE_DELIVERED:
                                listener.onMessageDelivered((Message) packet.getPayload());
                                break;
                            case MESSAGE_READ:
                                Map<String, Long> readData = (Map<String, Long>) packet.getPayload();
                                listener.onMessageRead(readData.get("readerId"));
                                break;
                            case TYPING_INDICATOR:
                                Map<String, Object> typingData = (Map<String, Object>) packet.getPayload();
                                Long sId = (Long) typingData.get("senderId");
                                String sName = (String) typingData.get("senderUsername");
                                Long gId = (Long) typingData.get("groupId");
                                Boolean isTyping = (Boolean) typingData.get("isTyping");
                                listener.onTypingIndicator(sId, sName, gId, isTyping);
                                break;
                            case GET_CHAT_HISTORY_RESPONSE:
                                listener.onChatHistoryReceived((List<Message>) packet.getPayload());
                                break;
                            case CREATE_GROUP_RESPONSE:
                                listener.onGroupCreated((ChatGroup) packet.getPayload());
                                break;
                            case GET_USER_GROUPS_RESPONSE:
                                listener.onUserGroupsReceived((List<ChatGroup>) packet.getPayload());
                                break;
                            case FILE_UPLOAD_RESPONSE:
                                Map<String, Object> fileData = (Map<String, Object>) packet.getPayload();
                                listener.onFileUploadResponse((Message) fileData.get("message"), (SharedFile) fileData.get("file"));
                                break;
                            case FILE_DOWNLOAD_RESPONSE:
                                Map<String, Object> downloadData = (Map<String, Object>) packet.getPayload();
                                listener.onFileDownloadResponse((String) downloadData.get("storedName"), (byte[]) downloadData.get("fileData"));
                                break;
                            case USER_STATUS_CHANGE:
                                Map<String, Object> statusData = (Map<String, Object>) packet.getPayload();
                                Long uId = (Long) statusData.get("userId");
                                String st = (String) statusData.get("status");
                                listener.onUserStatusChanged(uId, st);
                                break;
                            case ERROR_RESPONSE:
                                listener.onErrorReceived(packet.getMessage());
                                break;
                            default:
                                break;
                        }
                    } catch (Exception e) {
                        LoggerUtil.error("Error invoking listener", e);
                    }
                }
            }
        });
    }

    private void notifyError(String errorMsg) {
        SwingUtilities.invokeLater(() -> {
            synchronized (listeners) {
                for (ServerEventListener listener : listeners) {
                    listener.onErrorReceived(errorMsg);
                }
            }
        });
    }

    public void disconnect() {
        connected = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            // ignore
        }
    }

    public User getCurrentUser() { return currentUser; }
    public String getSessionToken() { return sessionToken; }
    public boolean isConnected() { return connected; }
}

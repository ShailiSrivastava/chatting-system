package com.chat.server.network;

import com.chat.common.model.*;
import com.chat.common.protocol.Packet;
import com.chat.common.protocol.PacketFactory;
import com.chat.common.protocol.PacketType;
import com.chat.common.util.LoggerUtil;
import com.chat.server.service.*;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    private final AuthService authService;
    private final UserService userService;
    private final MessageService messageService;
    private final GroupService groupService;
    private final FileService fileService;
    private final NotificationService notificationService;

    private User currentUser;
    private String sessionToken;
    private volatile boolean running = true;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.authService = new AuthService();
        this.userService = new UserService();
        this.messageService = new MessageService();
        this.groupService = new GroupService();
        this.fileService = new FileService();
        this.notificationService = new NotificationService();
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            LoggerUtil.info("Client handler started for socket: " + socket.getRemoteSocketAddress());

            while (running && !socket.isClosed()) {
                Object obj = in.readObject();
                if (obj instanceof Packet) {
                    Packet request = (Packet) obj;
                    handlePacket(request);
                }
            }
        } catch (EOFException | SocketException e) {
            LoggerUtil.info("Client disconnected gracefully: " + (currentUser != null ? currentUser.getUsername() : socket.getRemoteSocketAddress()));
        } catch (Exception e) {
            LoggerUtil.error("Error processing client connection", e);
        } finally {
            closeConnection();
        }
    }

    private void handlePacket(Packet packet) {
        PacketType type = packet.getType();

        try {
            if (type == PacketType.REGISTER_REQUEST) {
                handleRegister(packet);
                return;
            } else if (type == PacketType.LOGIN_REQUEST) {
                handleLogin(packet);
                return;
            }

            // Authenticate session for all other request types
            User authenticatedUser = authService.validateSession(packet.getSessionToken());
            if (authenticatedUser == null) {
                sendPacket(PacketFactory.createErrorPacket(PacketType.ERROR_RESPONSE, "Invalid or expired session. Please log in again."));
                return;
            }

            this.currentUser = authenticatedUser;
            this.sessionToken = packet.getSessionToken();

            switch (type) {
                case LOGOUT_REQUEST:
                    handleLogout();
                    break;
                case GET_USER_LIST_REQUEST:
                    handleGetUserList();
                    break;
                case SEARCH_USER_REQUEST:
                    handleSearchUser(packet);
                    break;
                case UPDATE_PROFILE_REQUEST:
                    handleUpdateProfile(packet);
                    break;
                case CHAT_MESSAGE:
                    handleChatMessage(packet);
                    break;
                case TYPING_INDICATOR:
                    handleTypingIndicator(packet);
                    break;
                case MESSAGE_READ:
                    handleMessageRead(packet);
                    break;
                case GET_CHAT_HISTORY_REQUEST:
                    handleGetChatHistory(packet);
                    break;
                case CREATE_GROUP_REQUEST:
                    handleCreateGroup(packet);
                    break;
                case GET_USER_GROUPS_REQUEST:
                    handleGetUserGroups();
                    break;
                case FILE_UPLOAD_REQUEST:
                    handleFileUpload(packet);
                    break;
                case FILE_DOWNLOAD_REQUEST:
                    handleFileDownload(packet);
                    break;
                default:
                    sendPacket(PacketFactory.createErrorPacket("Unsupported operation: " + type));
            }
        } catch (Exception e) {
            LoggerUtil.error("Error handling packet " + type, e);
            sendPacket(PacketFactory.createErrorPacket("Server Error: " + e.getMessage()));
        }
    }

    private void handleRegister(Packet packet) {
        Map<String, String> data = (Map<String, String>) packet.getPayload();
        try {
            User user = authService.register(data.get("username"), data.get("email"), data.get("password"));
            sendPacket(PacketFactory.createSuccessPacket(PacketType.REGISTER_RESPONSE, user));
        } catch (Exception e) {
            sendPacket(PacketFactory.createErrorPacket(PacketType.REGISTER_RESPONSE, e.getMessage()));
        }
    }

    private void handleLogin(Packet packet) {
        Map<String, String> data = (Map<String, String>) packet.getPayload();
        try {
            String token = authService.login(data.get("usernameOrEmail"), data.get("password"));
            User user = authService.validateSession(token);
            this.currentUser = user;
            this.sessionToken = token;

            // Register active connection
            ClientManager.getInstance().registerClient(user.getId(), this);

            // Response with User object & Token
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("user", user);
            responseData.put("token", token);
            sendPacket(PacketFactory.createSuccessPacket(PacketType.LOGIN_RESPONSE, token, responseData));

            // Broadcast online status to other users
            ClientManager.getInstance().broadcastStatusChange(user.getId(), UserStatus.ONLINE);

            // Deliver pending offline messages
            deliverOfflineMessages();

        } catch (Exception e) {
            sendPacket(PacketFactory.createErrorPacket(PacketType.LOGIN_RESPONSE, e.getMessage()));
        }
    }

    private void handleLogout() {
        if (sessionToken != null) {
            authService.logout(sessionToken);
        }
        if (currentUser != null) {
            ClientManager.getInstance().unregisterClient(currentUser.getId());
            ClientManager.getInstance().broadcastStatusChange(currentUser.getId(), UserStatus.OFFLINE);
        }
        sendPacket(PacketFactory.createSuccessPacket(PacketType.LOGOUT_RESPONSE, "Logged out successfully."));
        this.running = false;
    }

    private void handleGetUserList() {
        List<User> users = userService.getAllUsers();
        for (User u : users) {
            if (ClientManager.getInstance().isUserOnline(u.getId())) {
                u.setStatus(UserStatus.ONLINE);
            } else {
                u.setStatus(UserStatus.OFFLINE);
            }
        }
        sendPacket(PacketFactory.createSuccessPacket(PacketType.GET_USER_LIST_RESPONSE, users));
    }

    private void handleSearchUser(Packet packet) {
        String query = (String) packet.getPayload();
        List<User> results = userService.searchUsers(query);
        sendPacket(PacketFactory.createSuccessPacket(PacketType.SEARCH_USER_RESPONSE, results));
    }

    private void handleUpdateProfile(Packet packet) {
        Map<String, String> data = (Map<String, String>) packet.getPayload();
        try {
            if (data.containsKey("newPassword")) {
                userService.changePassword(currentUser.getId(), data.get("currentPassword"), data.get("newPassword"));
            }
            if (data.containsKey("bio") || data.containsKey("avatarPath")) {
                userService.updateProfile(currentUser.getId(), data.get("bio"), data.get("avatarPath"));
            }
            User updated = userService.getUserById(currentUser.getId());
            this.currentUser = updated;
            sendPacket(PacketFactory.createSuccessPacket(PacketType.UPDATE_PROFILE_RESPONSE, updated));
        } catch (Exception e) {
            sendPacket(PacketFactory.createErrorPacket(PacketType.UPDATE_PROFILE_RESPONSE, e.getMessage()));
        }
    }

    private void handleChatMessage(Packet packet) {
        Message msg = (Message) packet.getPayload();
        msg.setSenderId(currentUser.getId());
        msg.setSenderUsername(currentUser.getUsername());

        Message savedMsg = messageService.sendMessage(msg);
        if (savedMsg == null) {
            sendPacket(PacketFactory.createErrorPacket("Failed to deliver message."));
            return;
        }

        Packet chatPacket = PacketFactory.createSuccessPacket(PacketType.CHAT_MESSAGE, savedMsg);

        if (savedMsg.isGroupMessage()) {
            List<User> members = groupService.getGroupMembers(savedMsg.getGroupId());
            for (User member : members) {
                if (!member.getId().equals(currentUser.getId())) {
                    ClientManager.getInstance().sendDirectPacket(member.getId(), chatPacket);
                }
            }
        } else if (savedMsg.getReceiverId() != null) {
            boolean delivered = ClientManager.getInstance().sendDirectPacket(savedMsg.getReceiverId(), chatPacket);
            if (delivered) {
                messageService.updateStatus(savedMsg.getId(), MessageStatus.DELIVERED);
                savedMsg.setStatus(MessageStatus.DELIVERED);
                sendPacket(PacketFactory.createSuccessPacket(PacketType.MESSAGE_DELIVERED, savedMsg));
            }
        }
    }

    private void handleTypingIndicator(Packet packet) {
        Map<String, Object> data = (Map<String, Object>) packet.getPayload();
        Long receiverId = (Long) data.get("receiverId");
        Long groupId = (Long) data.get("groupId");
        Boolean isTyping = (Boolean) data.get("isTyping");

        Map<String, Object> payload = new HashMap<>();
        payload.put("senderId", currentUser.getId());
        payload.put("senderUsername", currentUser.getUsername());
        payload.put("receiverId", receiverId);
        payload.put("groupId", groupId);
        payload.put("isTyping", isTyping);

        Packet typingPacket = PacketFactory.createSuccessPacket(PacketType.TYPING_INDICATOR, payload);

        if (groupId != null && groupId > 0) {
            List<User> members = groupService.getGroupMembers(groupId);
            for (User member : members) {
                if (!member.getId().equals(currentUser.getId())) {
                    ClientManager.getInstance().sendDirectPacket(member.getId(), typingPacket);
                }
            }
        } else if (receiverId != null) {
            ClientManager.getInstance().sendDirectPacket(receiverId, typingPacket);
        }
    }

    private void handleMessageRead(Packet packet) {
        Map<String, Long> data = (Map<String, Long>) packet.getPayload();
        Long senderId = data.get("senderId");
        if (senderId != null) {
            messageService.markMessagesAsRead(senderId, currentUser.getId());

            Map<String, Long> payload = new HashMap<>();
            payload.put("readerId", currentUser.getId());
            payload.put("senderId", senderId);
            Packet readPacket = PacketFactory.createSuccessPacket(PacketType.MESSAGE_READ, payload);

            ClientManager.getInstance().sendDirectPacket(senderId, readPacket);
        }
    }

    private void handleGetChatHistory(Packet packet) {
        Map<String, Long> data = (Map<String, Long>) packet.getPayload();
        Long otherUserId = data.get("otherUserId");
        Long groupId = data.get("groupId");

        List<Message> history;
        if (groupId != null && groupId > 0) {
            history = messageService.getGroupChatHistory(groupId);
        } else {
            history = messageService.getPrivateChatHistory(currentUser.getId(), otherUserId);
            messageService.markMessagesAsRead(otherUserId, currentUser.getId());
        }

        sendPacket(PacketFactory.createSuccessPacket(PacketType.GET_CHAT_HISTORY_RESPONSE, history));
    }

    private void handleCreateGroup(Packet packet) {
        Map<String, Object> data = (Map<String, Object>) packet.getPayload();
        String groupName = (String) data.get("name");
        String description = (String) data.get("description");
        List<Long> memberIds = (List<Long>) data.get("memberIds");

        try {
            ChatGroup group = groupService.createGroup(groupName, description, currentUser.getId(), memberIds);
            sendPacket(PacketFactory.createSuccessPacket(PacketType.CREATE_GROUP_RESPONSE, group));

            // Notify added group members
            List<User> members = groupService.getGroupMembers(group.getId());
            Packet notifyPacket = PacketFactory.createSuccessPacket(PacketType.CREATE_GROUP_RESPONSE, group);
            for (User member : members) {
                if (!member.getId().equals(currentUser.getId())) {
                    ClientManager.getInstance().sendDirectPacket(member.getId(), notifyPacket);
                }
            }
        } catch (Exception e) {
            sendPacket(PacketFactory.createErrorPacket(PacketType.CREATE_GROUP_RESPONSE, e.getMessage()));
        }
    }

    private void handleGetUserGroups() {
        List<ChatGroup> groups = groupService.getUserGroups(currentUser.getId());
        sendPacket(PacketFactory.createSuccessPacket(PacketType.GET_USER_GROUPS_RESPONSE, groups));
    }

    private void handleFileUpload(Packet packet) {
        Map<String, Object> data = (Map<String, Object>) packet.getPayload();
        String fileName = (String) data.get("fileName");
        String fileType = (String) data.get("fileType");
        byte[] fileBytes = (byte[]) data.get("fileData");
        Long receiverId = (Long) data.get("receiverId");
        Long groupId = (Long) data.get("groupId");

        try {
            // First create message placeholder
            Message msg = new Message();
            msg.setSenderId(currentUser.getId());
            msg.setSenderUsername(currentUser.getUsername());
            msg.setReceiverId(receiverId);
            msg.setGroupId(groupId);
            msg.setContent("[File Attachment] " + fileName);
            msg.setMessageType(MessageType.FILE);

            Message savedMsg = messageService.sendMessage(msg);

            // Save file binary to server disk & link with message
            SharedFile sharedFile = fileService.saveFile(fileName, fileType, fileBytes, savedMsg.getId());

            Map<String, Object> responsePayload = new HashMap<>();
            responsePayload.put("message", savedMsg);
            responsePayload.put("file", sharedFile);

            Packet fileMsgPacket = PacketFactory.createSuccessPacket(PacketType.FILE_UPLOAD_RESPONSE, responsePayload);
            sendPacket(fileMsgPacket); // Confirm to sender

            if (groupId != null && groupId > 0) {
                List<User> members = groupService.getGroupMembers(groupId);
                for (User member : members) {
                    if (!member.getId().equals(currentUser.getId())) {
                        ClientManager.getInstance().sendDirectPacket(member.getId(), fileMsgPacket);
                    }
                }
            } else if (receiverId != null) {
                ClientManager.getInstance().sendDirectPacket(receiverId, fileMsgPacket);
            }
        } catch (Exception e) {
            sendPacket(PacketFactory.createErrorPacket(PacketType.FILE_UPLOAD_RESPONSE, "File upload failed: " + e.getMessage()));
        }
    }

    private void handleFileDownload(Packet packet) {
        Map<String, Object> data = (Map<String, Object>) packet.getPayload();
        String storedName = (String) data.get("storedName");
        try {
            byte[] fileBytes = fileService.downloadFile(storedName);
            Map<String, Object> responsePayload = new HashMap<>();
            responsePayload.put("storedName", storedName);
            responsePayload.put("fileData", fileBytes);
            sendPacket(PacketFactory.createSuccessPacket(PacketType.FILE_DOWNLOAD_RESPONSE, responsePayload));
        } catch (Exception e) {
            sendPacket(PacketFactory.createErrorPacket(PacketType.FILE_DOWNLOAD_RESPONSE, "Failed to download file: " + e.getMessage()));
        }
    }

    private void deliverOfflineMessages() {
        if (currentUser == null) return;
        List<Message> pending = messageService.getPendingOfflineMessages(currentUser.getId());
        if (!pending.isEmpty()) {
            for (Message m : pending) {
                sendPacket(PacketFactory.createSuccessPacket(PacketType.CHAT_MESSAGE, m));
            }
            messageService.markMessagesAsDelivered(currentUser.getId());
            LoggerUtil.info("Delivered " + pending.size() + " pending offline messages to " + currentUser.getUsername());
        }
    }

    public synchronized void sendPacket(Packet packet) {
        try {
            if (out != null && !socket.isClosed()) {
                out.writeObject(packet);
                out.flush();
            }
        } catch (IOException e) {
            LoggerUtil.error("Error sending packet to user: " + (currentUser != null ? currentUser.getUsername() : "unknown"), e);
        }
    }

    private void closeConnection() {
        running = false;
        if (currentUser != null) {
            ClientManager.getInstance().unregisterClient(currentUser.getId());
            ClientManager.getInstance().broadcastStatusChange(currentUser.getId(), UserStatus.OFFLINE);
            if (sessionToken != null) {
                authService.logout(sessionToken);
            }
        }
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            // ignore
        }
    }
}

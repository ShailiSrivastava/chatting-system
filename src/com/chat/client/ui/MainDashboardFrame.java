package com.chat.client.ui;

import com.chat.client.listener.ServerEventListener;
import com.chat.client.network.ChatClient;
import com.chat.common.model.*;
import com.chat.common.util.Constants;
import com.chat.common.util.FileUtil;
import com.chat.common.util.LoggerUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class MainDashboardFrame extends JFrame implements ServerEventListener {

    private final ChatClient client;
    private final User currentUser;

    // Active Selection State
    private User activeUserTarget = null;
    private ChatGroup activeGroupTarget = null;

    // Data lists
    private List<User> allUsers = new ArrayList<>();
    private List<ChatGroup> userGroups = new ArrayList<>();

    // UI Components
    private DefaultListModel<User> userListModel;
    private JList<User> userJList;

    private DefaultListModel<ChatGroup> groupListModel;
    private JList<ChatGroup> groupJList;

    private JLabel chatHeaderLabel;
    private JLabel chatStatusLabel;
    private JPanel chatMessagesPanel;
    private JScrollPane chatScrollPane;
    private JTextField messageInputField;
    private JCheckBox aesCheckBox;
    private JLabel typingIndicatorLabel;

    private javax.swing.Timer typingTimer;
    private boolean isCurrentlyTyping = false;

    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    public MainDashboardFrame(ChatClient client, User currentUser) {
        this.client = client;
        this.currentUser = currentUser;
        this.client.addListener(this);

        setTitle("Antigravity Enterprise Real-Time Chat - [" + currentUser.getUsername() + "]");
        setSize(1080, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
        loadInitialData();
    }

    private void initUI() {
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(UIComponentFactory.COLOR_BACKGROUND);

        // Sidebar Panel (Left)
        JPanel sidebar = createSidebarPanel();
        sidebar.setPreferredSize(new Dimension(320, 720));
        rootPanel.add(sidebar, BorderLayout.WEST);

        // Chat Container (Right)
        JPanel chatContainer = createChatContainerPanel();
        rootPanel.add(chatContainer, BorderLayout.CENTER);

        add(rootPanel);
    }

    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(UIComponentFactory.COLOR_SIDEBAR);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UIComponentFactory.COLOR_SURFACE));

        // Header Card (User Profile info)
        JPanel userHeader = new JPanel(new BorderLayout(8, 8));
        userHeader.setBackground(UIComponentFactory.COLOR_SURFACE);
        userHeader.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel nameLbl = new JLabel(currentUser.getUsername());
        nameLbl.setFont(UIComponentFactory.FONT_HEADER);
        nameLbl.setForeground(UIComponentFactory.COLOR_TEXT);

        JLabel emailLbl = new JLabel(currentUser.getEmail());
        emailLbl.setFont(UIComponentFactory.FONT_SMALL);
        emailLbl.setForeground(UIComponentFactory.COLOR_TEXT_MUTED);

        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        infoPanel.setOpaque(false);
        infoPanel.add(nameLbl);
        infoPanel.add(emailLbl);

        userHeader.add(infoPanel, BorderLayout.CENTER);

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        headerActions.setOpaque(false);

        JButton profileBtn = new JButton("⚙");
        profileBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        profileBtn.setForeground(UIComponentFactory.COLOR_TEXT);
        profileBtn.setContentAreaFilled(false);
        profileBtn.setBorderPainted(false);
        profileBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        profileBtn.addActionListener(e -> new ProfileDialog(this, client).setVisible(true));
        headerActions.add(profileBtn);

        JButton logoutBtn = new JButton("🚪");
        logoutBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        logoutBtn.setForeground(UIComponentFactory.COLOR_TEXT);
        logoutBtn.setContentAreaFilled(false);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> performLogout());
        headerActions.add(logoutBtn);

        userHeader.add(headerActions, BorderLayout.EAST);
        sidebar.add(userHeader, BorderLayout.NORTH);

        // Center Content (Search + Tabs + Contact List)
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);

        // Search Bar
        JTextField searchField = UIComponentFactory.createStyledTextField("Search users or contacts...");
        searchField.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(10, 12, 10, 12),
                searchField.getBorder()
        ));
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String q = searchField.getText().trim();
                if (q.isEmpty()) {
                    client.requestUserList();
                } else {
                    client.searchUser(q);
                }
            }
        });
        centerPanel.add(searchField, BorderLayout.NORTH);

        // Tabbed Pane for DMs and Groups
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UIComponentFactory.FONT_SMALL);
        tabbedPane.setBackground(UIComponentFactory.COLOR_SIDEBAR);
        tabbedPane.setForeground(UIComponentFactory.COLOR_TEXT);

        // DM User List
        userListModel = new DefaultListModel<>();
        userJList = new JList<>(userListModel);
        userJList.setBackground(UIComponentFactory.COLOR_SIDEBAR);
        userJList.setCellRenderer(new UserListCellRenderer());
        userJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                User selected = userJList.getSelectedValue();
                if (selected != null) {
                    groupJList.clearSelection();
                    selectDirectUserChat(selected);
                }
            }
        });
        JScrollPane userScroll = new JScrollPane(userJList);
        userScroll.setBorder(BorderFactory.createEmptyBorder());
        tabbedPane.addTab("Direct Messages", userScroll);

        // Group Chat List
        groupListModel = new DefaultListModel<>();
        groupJList = new JList<>(groupListModel);
        groupJList.setBackground(UIComponentFactory.COLOR_SIDEBAR);
        groupJList.setCellRenderer(new GroupListCellRenderer());
        groupJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ChatGroup selected = groupJList.getSelectedValue();
                if (selected != null) {
                    userJList.clearSelection();
                    selectGroupChat(selected);
                }
            }
        });
        JScrollPane groupScroll = new JScrollPane(groupJList);
        groupScroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel groupTabContainer = new JPanel(new BorderLayout());
        groupTabContainer.add(groupScroll, BorderLayout.CENTER);

        JButton newGroupBtn = UIComponentFactory.createStyledButton("+ New Group", UIComponentFactory.COLOR_PRIMARY, UIComponentFactory.COLOR_BACKGROUND);
        newGroupBtn.setPreferredSize(new Dimension(280, 36));
        newGroupBtn.addActionListener(e -> new CreateGroupDialog(this, client, allUsers).setVisible(true));

        JPanel newGroupBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        newGroupBtnPanel.setOpaque(false);
        newGroupBtnPanel.add(newGroupBtn);
        groupTabContainer.add(newGroupBtnPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Group Chats", groupTabContainer);

        centerPanel.add(tabbedPane, BorderLayout.CENTER);
        sidebar.add(centerPanel, BorderLayout.CENTER);

        return sidebar;
    }

    private JPanel createChatContainerPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBackground(UIComponentFactory.COLOR_BACKGROUND);

        // Active Chat Header
        JPanel chatHeader = new JPanel(new BorderLayout());
        chatHeader.setBackground(UIComponentFactory.COLOR_SURFACE);
        chatHeader.setBorder(new EmptyBorder(12, 20, 12, 20));

        chatHeaderLabel = new JLabel("Select a conversation to start chatting");
        chatHeaderLabel.setFont(UIComponentFactory.FONT_HEADER);
        chatHeaderLabel.setForeground(UIComponentFactory.COLOR_TEXT);

        chatStatusLabel = new JLabel("");
        chatStatusLabel.setFont(UIComponentFactory.FONT_SMALL);
        chatStatusLabel.setForeground(UIComponentFactory.COLOR_ONLINE);

        JPanel headerTextPanel = new JPanel(new GridLayout(2, 1));
        headerTextPanel.setOpaque(false);
        headerTextPanel.add(chatHeaderLabel);
        headerTextPanel.add(chatStatusLabel);

        chatHeader.add(headerTextPanel, BorderLayout.CENTER);
        chatPanel.add(chatHeader, BorderLayout.NORTH);

        // Center Messages View
        chatMessagesPanel = new JPanel();
        chatMessagesPanel.setLayout(new BoxLayout(chatMessagesPanel, BoxLayout.Y_AXIS));
        chatMessagesPanel.setBackground(UIComponentFactory.COLOR_BACKGROUND);
        chatMessagesPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        chatScrollPane = new JScrollPane(chatMessagesPanel);
        chatScrollPane.setBorder(BorderFactory.createEmptyBorder());
        chatScrollPane.getViewport().setBackground(UIComponentFactory.COLOR_BACKGROUND);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);

        // Bottom Input Container
        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.setOpaque(false);

        // Emoji Quick Selector Row
        JPanel emojiQuickBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        emojiQuickBar.setOpaque(false);
        emojiQuickBar.setBorder(new EmptyBorder(0, 16, 4, 16));

        String[] quickEmojis = {"😀", "😂", "😍", "👍", "❤️", "🔥", "🚀", "🎉", "💡"};
        for (String em : quickEmojis) {
            JButton emBtn = new JButton(em);
            emBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
            emBtn.setForeground(UIComponentFactory.COLOR_TEXT);
            emBtn.setContentAreaFilled(false);
            emBtn.setBorderPainted(false);
            emBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            emBtn.addActionListener(e -> {
                messageInputField.setText(messageInputField.getText() + em);
                messageInputField.requestFocus();
            });
            emojiQuickBar.add(emBtn);
        }
        bottomContainer.add(emojiQuickBar, BorderLayout.NORTH);

        // Typing Indicator Bar
        typingIndicatorLabel = new JLabel(" ");
        typingIndicatorLabel.setFont(UIComponentFactory.FONT_SMALL);
        typingIndicatorLabel.setForeground(UIComponentFactory.COLOR_PRIMARY);
        typingIndicatorLabel.setBorder(new EmptyBorder(4, 20, 4, 20));
        bottomContainer.add(typingIndicatorLabel, BorderLayout.NORTH);

        // Input Controls Row
        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setBackground(UIComponentFactory.COLOR_SURFACE);
        inputRow.setBorder(new EmptyBorder(10, 16, 10, 16));

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        leftActions.setOpaque(false);

        JButton fileBtn = new JButton("📎");
        fileBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        fileBtn.setForeground(UIComponentFactory.COLOR_TEXT);
        fileBtn.setContentAreaFilled(false);
        fileBtn.setBorderPainted(false);
        fileBtn.setToolTipText("Send File Attachment");
        fileBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        fileBtn.addActionListener(e -> selectAndSendFile());
        leftActions.add(fileBtn);

        aesCheckBox = new JCheckBox("🔐 AES");
        aesCheckBox.setFont(UIComponentFactory.FONT_SMALL);
        aesCheckBox.setForeground(UIComponentFactory.COLOR_TEXT);
        aesCheckBox.setOpaque(false);
        aesCheckBox.setToolTipText("Enable End-to-End AES Payload Encryption");
        leftActions.add(aesCheckBox);

        inputRow.add(leftActions, BorderLayout.WEST);

        messageInputField = UIComponentFactory.createStyledTextField("Type a message...");
        messageInputField.addActionListener(e -> performSendMessage());
        messageInputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                handleTypingState();
            }
        });
        inputRow.add(messageInputField, BorderLayout.CENTER);

        JButton sendBtn = UIComponentFactory.createStyledButton("Send", UIComponentFactory.COLOR_PRIMARY, UIComponentFactory.COLOR_BACKGROUND);
        sendBtn.setPreferredSize(new Dimension(80, 36));
        sendBtn.addActionListener(e -> performSendMessage());
        inputRow.add(sendBtn, BorderLayout.EAST);

        bottomContainer.add(inputRow, BorderLayout.SOUTH);
        chatPanel.add(bottomContainer, BorderLayout.SOUTH);

        return chatPanel;
    }

    private void loadInitialData() {
        client.requestUserList();
        client.requestUserGroups();
    }

    private void selectDirectUserChat(User targetUser) {
        this.activeUserTarget = targetUser;
        this.activeGroupTarget = null;

        chatHeaderLabel.setText(targetUser.getUsername());
        chatStatusLabel.setText(targetUser.getStatus() == UserStatus.ONLINE ? "● Online" : "○ Offline");

        client.requestChatHistory(targetUser.getId(), null);
    }

    private void selectGroupChat(ChatGroup group) {
        this.activeGroupTarget = group;
        this.activeUserTarget = null;

        chatHeaderLabel.setText("👥 " + group.getName());
        chatStatusLabel.setText(group.getDescription() != null ? group.getDescription() : "Group Conversation");

        client.requestChatHistory(null, group.getId());
    }

    private void performSendMessage() {
        String text = messageInputField.getText().trim();
        if (text.isEmpty()) return;

        if (activeUserTarget == null && activeGroupTarget == null) {
            JOptionPane.showMessageDialog(this, "Please select a user or group to send a message.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Message msg = new Message();
        msg.setSenderId(currentUser.getId());
        msg.setSenderUsername(currentUser.getUsername());
        msg.setContent(text);
        msg.setEncrypted(aesCheckBox.isSelected());
        msg.setMessageType(MessageType.TEXT);

        if (activeGroupTarget != null) {
            msg.setGroupId(activeGroupTarget.getId());
        } else if (activeUserTarget != null) {
            msg.setReceiverId(activeUserTarget.getId());
        }

        client.sendMessage(msg);
        messageInputField.setText("");

        // Render sent bubble immediately for instant feedback
        appendMessageBubble(msg, true);
    }

    private void selectAndSendFile() {
        if (activeUserTarget == null && activeGroupTarget == null) {
            JOptionPane.showMessageDialog(this, "Please select a recipient first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        int ret = chooser.showOpenDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (file.length() > Constants.MAX_FILE_SIZE_BYTES) {
                JOptionPane.showMessageDialog(this, "File size exceeds 50MB limit.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            new Thread(() -> {
                try {
                    byte[] fileBytes = new byte[(int) file.length()];
                    try (FileInputStream fis = new FileInputStream(file)) {
                        fis.read(fileBytes);
                    }
                    Long rId = activeUserTarget != null ? activeUserTarget.getId() : null;
                    Long gId = activeGroupTarget != null ? activeGroupTarget.getId() : null;
                    client.uploadFile(file.getName(), getFileType(file.getName()), fileBytes, rId, gId);
                } catch (Exception ex) {
                    LoggerUtil.error("Failed to read file", ex);
                }
            }).start();
        }
    }

    private String getFileType(String fileName) {
        int i = fileName.lastIndexOf('.');
        return i > 0 ? fileName.substring(i + 1).toLowerCase() : "bin";
    }

    private void handleTypingState() {
        if (activeUserTarget == null && activeGroupTarget == null) return;

        if (!isCurrentlyTyping) {
            isCurrentlyTyping = true;
            Long rId = activeUserTarget != null ? activeUserTarget.getId() : null;
            Long gId = activeGroupTarget != null ? activeGroupTarget.getId() : null;
            client.sendTypingIndicator(rId, gId, true);
        }

        if (typingTimer != null && typingTimer.isRunning()) {
            typingTimer.restart();
        } else {
            typingTimer = new javax.swing.Timer(2000, e -> {
                isCurrentlyTyping = false;
                Long rId = activeUserTarget != null ? activeUserTarget.getId() : null;
                Long gId = activeGroupTarget != null ? activeGroupTarget.getId() : null;
                client.sendTypingIndicator(rId, gId, false);
            });
            typingTimer.setRepeats(false);
            typingTimer.start();
        }
    }

    private void appendMessageBubble(Message msg, boolean isSentByMe) {
        JPanel bubbleRow = new JPanel(new FlowLayout(isSentByMe ? FlowLayout.RIGHT : FlowLayout.LEFT));
        bubbleRow.setOpaque(false);

        Color bubbleBg = isSentByMe ? UIComponentFactory.COLOR_SENT_BUBBLE : UIComponentFactory.COLOR_RECV_BUBBLE;
        JPanel bubble = UIComponentFactory.createRoundedPanel(bubbleBg, 14);
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(new EmptyBorder(8, 12, 8, 12));

        if (!isSentByMe && msg.isGroupMessage()) {
            JLabel senderLbl = new JLabel(msg.getSenderUsername() != null ? msg.getSenderUsername() : "User");
            senderLbl.setFont(UIComponentFactory.FONT_SMALL);
            senderLbl.setForeground(UIComponentFactory.COLOR_PRIMARY);
            bubble.add(senderLbl);
            bubble.add(Box.createVerticalStrut(2));
        }

        JLabel contentLbl = new JLabel("<html><body style='width: 260px;'>" + msg.getContent() + "</body></html>");
        contentLbl.setFont(UIComponentFactory.FONT_MAIN);
        contentLbl.setForeground(UIComponentFactory.COLOR_TEXT);
        bubble.add(contentLbl);
        bubble.add(Box.createVerticalStrut(4));

        String timeStr = msg.getTimestamp() != null ? timeFormat.format(msg.getTimestamp()) : timeFormat.format(new Date());
        String statusCheck = isSentByMe ? (msg.getStatus() == MessageStatus.READ ? " ✓✓" : " ✓") : "";

        JLabel footerLbl = new JLabel(timeStr + statusCheck + (msg.isEncrypted() ? " 🔐" : ""));
        footerLbl.setFont(UIComponentFactory.FONT_SMALL);
        footerLbl.setForeground(UIComponentFactory.COLOR_TEXT_MUTED);
        bubble.add(footerLbl);

        // Check if message is a file attachment
        if (msg.getMessageType() == MessageType.FILE) {
            JButton downloadBtn = new JButton("💾 Download File");
            downloadBtn.setFont(UIComponentFactory.FONT_SMALL);
            downloadBtn.setForeground(UIComponentFactory.COLOR_PRIMARY);
            downloadBtn.setContentAreaFilled(false);
            downloadBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            downloadBtn.addActionListener(e -> {
                String fileName = msg.getContent().replace("[File Attachment] ", "");
                client.downloadFile(fileName);
            });
            bubble.add(downloadBtn);
        }

        bubbleRow.add(bubble);
        chatMessagesPanel.add(bubbleRow);
        chatMessagesPanel.revalidate();
        chatMessagesPanel.repaint();

        // Scroll to bottom
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    private void renderChatHistory(List<Message> history) {
        chatMessagesPanel.removeAll();
        for (Message m : history) {
            boolean isSent = m.getSenderId().equals(currentUser.getId());
            appendMessageBubble(m, isSent);
        }
        chatMessagesPanel.revalidate();
        chatMessagesPanel.repaint();
    }

    private void performLogout() {
        int opt = JOptionPane.showConfirmDialog(this, "Are you sure you want to log out?", "Logout", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            client.logout();
            client.removeListener(this);
            LoginFrame loginFrame = new LoginFrame(new ChatClient());
            loginFrame.setVisible(true);
            this.dispose();
        }
    }

    // ServerEventListener Observer Callbacks
    @Override
    public void onUserListReceived(List<User> users) {
        this.allUsers = users;
        userListModel.clear();
        for (User u : users) {
            if (!u.getId().equals(currentUser.getId())) {
                userListModel.addElement(u);
            }
        }
    }

    @Override
    public void onUserSearchReceived(List<User> users) {
        userListModel.clear();
        for (User u : users) {
            if (!u.getId().equals(currentUser.getId())) {
                userListModel.addElement(u);
            }
        }
    }

    @Override
    public void onMessageReceived(Message message) {
        boolean isCurrentDMChat = activeUserTarget != null && message.getSenderId().equals(activeUserTarget.getId());
        boolean isCurrentGroupChat = activeGroupTarget != null && activeGroupTarget.getId().equals(message.getGroupId());

        if (isCurrentDMChat || isCurrentGroupChat) {
            appendMessageBubble(message, false);
            if (isCurrentDMChat) {
                client.markMessagesAsRead(activeUserTarget.getId());
            }
        } else {
            // Show system notification banner
            Toolkit.getDefaultToolkit().beep();
        }
    }

    @Override
    public void onMessageDelivered(Message message) {
        // Status updated
    }

    @Override
    public void onMessageRead(Long senderId) {
        // Handle read receipts
    }

    @Override
    public void onTypingIndicator(Long senderId, String senderUsername, Long groupId, boolean isTyping) {
        boolean isCurrentDM = activeUserTarget != null && activeUserTarget.getId().equals(senderId);
        boolean isCurrentGroup = activeGroupTarget != null && activeGroupTarget.getId().equals(groupId);

        if (isCurrentDM || isCurrentGroup) {
            if (isTyping) {
                typingIndicatorLabel.setText(senderUsername + " is typing...");
            } else {
                typingIndicatorLabel.setText(" ");
            }
        }
    }

    @Override
    public void onChatHistoryReceived(List<Message> history) {
        renderChatHistory(history);
    }

    @Override
    public void onGroupCreated(ChatGroup group) {
        userGroups.add(group);
        groupListModel.addElement(group);
    }

    @Override
    public void onUserGroupsReceived(List<ChatGroup> groups) {
        this.userGroups = groups;
        groupListModel.clear();
        for (ChatGroup g : groups) {
            groupListModel.addElement(g);
        }
    }

    @Override
    public void onFileUploadResponse(Message message, SharedFile sharedFile) {
        boolean isSentByMe = message.getSenderId().equals(currentUser.getId());
        appendMessageBubble(message, isSentByMe);
    }

    @Override
    public void onFileDownloadResponse(String storedName, byte[] fileData) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(storedName));
        int option = fileChooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File targetFile = fileChooser.getSelectedFile();
            try {
                FileUtil.saveFile(targetFile.getParent(), targetFile.getName(), fileData);
                JOptionPane.showMessageDialog(this, "File downloaded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to save downloaded file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void onUserStatusChanged(Long userId, String status) {
        for (int i = 0; i < userListModel.size(); i++) {
            User u = userListModel.get(i);
            if (u.getId().equals(userId)) {
                u.setStatus(UserStatus.valueOf(status));
                userListModel.set(i, u);
                break;
            }
        }
        if (activeUserTarget != null && activeUserTarget.getId().equals(userId)) {
            activeUserTarget.setStatus(UserStatus.valueOf(status));
            chatStatusLabel.setText(activeUserTarget.getStatus() == UserStatus.ONLINE ? "● Online" : "○ Offline");
        }
    }

    @Override
    public void onErrorReceived(String error) {
        LoggerUtil.warn("Client received error: " + error);
    }

    @Override public void onLoginSuccess(User user, String token) {}
    @Override public void onLoginFailure(String error) {}
    @Override public void onRegisterSuccess(User user) {}
    @Override public void onRegisterFailure(String error) {}

    // Custom Cell Renderers for Swing Lists
    private static class UserListCellRenderer extends JPanel implements ListCellRenderer<User> {
        private final JLabel nameLbl = new JLabel();
        private final JLabel statusDot = new JLabel("● ");

        public UserListCellRenderer() {
            setLayout(new BorderLayout(8, 0));
            setBorder(new EmptyBorder(10, 14, 10, 14));
            setOpaque(true);

            nameLbl.setFont(UIComponentFactory.FONT_MAIN);
            statusDot.setFont(UIComponentFactory.FONT_HEADER);

            add(statusDot, BorderLayout.WEST);
            add(nameLbl, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends User> list, User value, int index, boolean isSelected, boolean cellHasFocus) {
            nameLbl.setText(value.getUsername());
            if (value.getStatus() == UserStatus.ONLINE) {
                statusDot.setForeground(UIComponentFactory.COLOR_ONLINE);
            } else {
                statusDot.setForeground(UIComponentFactory.COLOR_OFFLINE);
            }

            if (isSelected) {
                setBackground(UIComponentFactory.COLOR_SURFACE_LIGHT);
                nameLbl.setForeground(UIComponentFactory.COLOR_TEXT);
            } else {
                setBackground(UIComponentFactory.COLOR_SIDEBAR);
                nameLbl.setForeground(UIComponentFactory.COLOR_TEXT_MUTED);
            }

            return this;
        }
    }

    private static class GroupListCellRenderer extends JPanel implements ListCellRenderer<ChatGroup> {
        private final JLabel iconLbl = new JLabel("👥 ");
        private final JLabel nameLbl = new JLabel();

        public GroupListCellRenderer() {
            setLayout(new BorderLayout(8, 0));
            setBorder(new EmptyBorder(10, 14, 10, 14));
            setOpaque(true);

            nameLbl.setFont(UIComponentFactory.FONT_MAIN);
            iconLbl.setFont(UIComponentFactory.FONT_HEADER);

            add(iconLbl, BorderLayout.WEST);
            add(nameLbl, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ChatGroup> list, ChatGroup value, int index, boolean isSelected, boolean cellHasFocus) {
            nameLbl.setText(value.getName());

            if (isSelected) {
                setBackground(UIComponentFactory.COLOR_SURFACE_LIGHT);
                nameLbl.setForeground(UIComponentFactory.COLOR_TEXT);
            } else {
                setBackground(UIComponentFactory.COLOR_SIDEBAR);
                nameLbl.setForeground(UIComponentFactory.COLOR_TEXT_MUTED);
            }

            return this;
        }
    }
}

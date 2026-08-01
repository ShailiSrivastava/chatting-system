package com.chat.client.ui;

import com.chat.client.listener.ServerEventListener;
import com.chat.client.network.ChatClient;
import com.chat.common.model.ChatGroup;
import com.chat.common.model.Message;
import com.chat.common.model.SharedFile;
import com.chat.common.model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class LoginFrame extends JFrame implements ServerEventListener {

    private final ChatClient client;

    private JTextField userField;
    private JPasswordField passField;
    private JButton loginButton;
    private JLabel statusLabel;
    private JLabel registerLink;

    public LoginFrame(ChatClient client) {
        this.client = client;
        this.client.addListener(this);

        setTitle("Antigravity Enterprise Chat - Sign In");
        setSize(420, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        initUI();
    }

    private void initUI() {
        JPanel mainPanel = UIComponentFactory.createRoundedPanel(UIComponentFactory.COLOR_BACKGROUND, 0);
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Title Header
        JLabel titleLabel = new JLabel("Welcome Back", SwingConstants.CENTER);
        titleLabel.setFont(UIComponentFactory.FONT_TITLE);
        titleLabel.setForeground(UIComponentFactory.COLOR_TEXT);
        mainPanel.add(titleLabel, gbc);

        gbc.gridy++;
        JLabel subtitleLabel = new JLabel("Sign in to your account to continue", SwingConstants.CENTER);
        subtitleLabel.setFont(UIComponentFactory.FONT_SMALL);
        subtitleLabel.setForeground(UIComponentFactory.COLOR_TEXT_MUTED);
        mainPanel.add(subtitleLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(20, 8, 4, 8);
        JLabel userLbl = new JLabel("Username or Email");
        userLbl.setFont(UIComponentFactory.FONT_SMALL);
        userLbl.setForeground(UIComponentFactory.COLOR_TEXT);
        mainPanel.add(userLbl, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 8, 8, 8);
        userField = UIComponentFactory.createStyledTextField("Enter username or email");
        mainPanel.add(userField, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(10, 8, 4, 8);
        JLabel passLbl = new JLabel("Password");
        passLbl.setFont(UIComponentFactory.FONT_SMALL);
        passLbl.setForeground(UIComponentFactory.COLOR_TEXT);
        mainPanel.add(passLbl, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 8, 12, 8);
        passField = UIComponentFactory.createStyledPasswordField();
        mainPanel.add(passField, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(10, 8, 10, 8);
        loginButton = UIComponentFactory.createStyledButton("Sign In", UIComponentFactory.COLOR_PRIMARY, UIComponentFactory.COLOR_BACKGROUND);
        loginButton.addActionListener(e -> performLogin());
        mainPanel.add(loginButton, gbc);

        gbc.gridy++;
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(UIComponentFactory.FONT_SMALL);
        statusLabel.setForeground(Color.RED);
        mainPanel.add(statusLabel, gbc);

        gbc.gridy++;
        registerLink = new JLabel("<html>Don't have an account? <font color='#89b4fa'><u>Register here</u></font></html>", SwingConstants.CENTER);
        registerLink.setFont(UIComponentFactory.FONT_SMALL);
        registerLink.setForeground(UIComponentFactory.COLOR_TEXT_MUTED);
        registerLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openRegisterFrame();
            }
        });
        mainPanel.add(registerLink, gbc);

        add(mainPanel);
    }

    private void performLogin() {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter username and password.");
            return;
        }

        statusLabel.setForeground(UIComponentFactory.COLOR_PRIMARY);
        statusLabel.setText("Connecting to server...");
        loginButton.setEnabled(false);

        new Thread(() -> {
            if (!client.isConnected()) {
                boolean connected = client.connect();
                if (!connected) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setForeground(Color.RED);
                        statusLabel.setText("Failed to connect to Chat Server.");
                        loginButton.setEnabled(true);
                    });
                    return;
                }
            }
            client.login(username, password);
        }).start();
    }

    private void openRegisterFrame() {
        client.removeListener(this);
        RegisterFrame registerFrame = new RegisterFrame(client);
        registerFrame.setVisible(true);
        this.dispose();
    }

    @Override
    public void onLoginSuccess(User user, String token) {
        client.removeListener(this);
        MainDashboardFrame dashboard = new MainDashboardFrame(client, user);
        dashboard.setVisible(true);
        this.dispose();
    }

    @Override
    public void onLoginFailure(String error) {
        statusLabel.setForeground(Color.RED);
        statusLabel.setText(error != null ? error : "Login failed.");
        loginButton.setEnabled(true);
    }

    @Override
    public void onErrorReceived(String error) {
        statusLabel.setForeground(Color.RED);
        statusLabel.setText(error);
        loginButton.setEnabled(true);
    }

    // Unused observer callbacks
    @Override public void onRegisterSuccess(User user) {}
    @Override public void onRegisterFailure(String error) {}
    @Override public void onUserListReceived(List<User> users) {}
    @Override public void onUserSearchReceived(List<User> users) {}
    @Override public void onMessageReceived(Message message) {}
    @Override public void onMessageDelivered(Message message) {}
    @Override public void onMessageRead(Long senderId) {}
    @Override public void onTypingIndicator(Long senderId, String senderUsername, Long groupId, boolean isTyping) {}
    @Override public void onChatHistoryReceived(List<Message> history) {}
    @Override public void onGroupCreated(ChatGroup group) {}
    @Override public void onUserGroupsReceived(List<ChatGroup> groups) {}
    @Override public void onFileUploadResponse(Message message, SharedFile sharedFile) {}
    @Override public void onFileDownloadResponse(String storedName, byte[] fileData) {}
    @Override public void onUserStatusChanged(Long userId, String status) {}
}

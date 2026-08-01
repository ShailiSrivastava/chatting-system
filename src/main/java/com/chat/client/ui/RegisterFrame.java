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

public class RegisterFrame extends JFrame implements ServerEventListener {

    private final ChatClient client;

    private JTextField userField;
    private JTextField emailField;
    private JPasswordField passField;
    private JPasswordField confirmPassField;
    private JButton registerButton;
    private JLabel statusLabel;

    public RegisterFrame(ChatClient client) {
        this.client = client;
        this.client.addListener(this);

        setTitle("Antigravity Enterprise Chat - Create Account");
        setSize(440, 600);
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
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel titleLabel = new JLabel("Create Account", SwingConstants.CENTER);
        titleLabel.setFont(UIComponentFactory.FONT_TITLE);
        titleLabel.setForeground(UIComponentFactory.COLOR_TEXT);
        mainPanel.add(titleLabel, gbc);

        gbc.gridy++;
        JLabel subtitleLabel = new JLabel("Join Antigravity Chat Platform", SwingConstants.CENTER);
        subtitleLabel.setFont(UIComponentFactory.FONT_SMALL);
        subtitleLabel.setForeground(UIComponentFactory.COLOR_TEXT_MUTED);
        mainPanel.add(subtitleLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(14, 8, 2, 8);
        JLabel userLbl = new JLabel("Username");
        userLbl.setFont(UIComponentFactory.FONT_SMALL);
        userLbl.setForeground(UIComponentFactory.COLOR_TEXT);
        mainPanel.add(userLbl, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 8, 6, 8);
        userField = UIComponentFactory.createStyledTextField("Choose username");
        mainPanel.add(userField, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(6, 8, 2, 8);
        JLabel emailLbl = new JLabel("Email Address");
        emailLbl.setFont(UIComponentFactory.FONT_SMALL);
        emailLbl.setForeground(UIComponentFactory.COLOR_TEXT);
        mainPanel.add(emailLbl, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 8, 6, 8);
        emailField = UIComponentFactory.createStyledTextField("Enter email");
        mainPanel.add(emailField, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(6, 8, 2, 8);
        JLabel passLbl = new JLabel("Password");
        passLbl.setFont(UIComponentFactory.FONT_SMALL);
        passLbl.setForeground(UIComponentFactory.COLOR_TEXT);
        mainPanel.add(passLbl, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 8, 6, 8);
        passField = UIComponentFactory.createStyledPasswordField();
        mainPanel.add(passField, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(6, 8, 2, 8);
        JLabel confirmPassLbl = new JLabel("Confirm Password");
        confirmPassLbl.setFont(UIComponentFactory.FONT_SMALL);
        confirmPassLbl.setForeground(UIComponentFactory.COLOR_TEXT);
        mainPanel.add(confirmPassLbl, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 8, 12, 8);
        confirmPassField = UIComponentFactory.createStyledPasswordField();
        mainPanel.add(confirmPassField, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(10, 8, 8, 8);
        registerButton = UIComponentFactory.createStyledButton("Register Now", UIComponentFactory.COLOR_PRIMARY, UIComponentFactory.COLOR_BACKGROUND);
        registerButton.addActionListener(e -> performRegister());
        mainPanel.add(registerButton, gbc);

        gbc.gridy++;
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(UIComponentFactory.FONT_SMALL);
        statusLabel.setForeground(Color.RED);
        mainPanel.add(statusLabel, gbc);

        gbc.gridy++;
        JLabel loginLink = new JLabel("<html>Already have an account? <font color='#89b4fa'><u>Sign in</u></font></html>", SwingConstants.CENTER);
        loginLink.setFont(UIComponentFactory.FONT_SMALL);
        loginLink.setForeground(UIComponentFactory.COLOR_TEXT_MUTED);
        loginLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openLoginFrame();
            }
        });
        mainPanel.add(loginLink, gbc);

        add(mainPanel);
    }

    private void performRegister() {
        String username = userField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passField.getPassword());
        String confirmPassword = new String(confirmPassField.getPassword());

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            statusLabel.setText("All fields are required.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            statusLabel.setText("Passwords do not match.");
            return;
        }

        statusLabel.setForeground(UIComponentFactory.COLOR_PRIMARY);
        statusLabel.setText("Connecting to server...");
        registerButton.setEnabled(false);

        new Thread(() -> {
            if (!client.isConnected()) {
                boolean connected = client.connect();
                if (!connected) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setForeground(Color.RED);
                        statusLabel.setText("Failed to connect to Chat Server.");
                        registerButton.setEnabled(true);
                    });
                    return;
                }
            }
            client.register(username, email, password);
        }).start();
    }

    private void openLoginFrame() {
        client.removeListener(this);
        LoginFrame loginFrame = new LoginFrame(client);
        loginFrame.setVisible(true);
        this.dispose();
    }

    @Override
    public void onRegisterSuccess(User user) {
        JOptionPane.showMessageDialog(this, "Registration Successful! Please sign in with your credentials.", "Success", JOptionPane.INFORMATION_MESSAGE);
        openLoginFrame();
    }

    @Override
    public void onRegisterFailure(String error) {
        statusLabel.setForeground(Color.RED);
        statusLabel.setText(error != null ? error : "Registration failed.");
        registerButton.setEnabled(true);
    }

    @Override
    public void onErrorReceived(String error) {
        statusLabel.setForeground(Color.RED);
        statusLabel.setText(error);
        registerButton.setEnabled(true);
    }

    // Unused observer callbacks
    @Override public void onLoginSuccess(User user, String token) {}
    @Override public void onLoginFailure(String error) {}
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

package com.chat.client.ui;

import com.chat.client.network.ChatClient;
import com.chat.common.model.User;

import javax.swing.*;
import java.awt.*;

public class ProfileDialog extends JDialog {

    private final ChatClient client;
    private final User currentUser;

    private JTextField bioField;
    private JPasswordField currentPassField;
    private JPasswordField newPassField;

    public ProfileDialog(Frame parent, ChatClient client) {
        super(parent, "My Profile & Settings", true);
        this.client = client;
        this.currentUser = client.getCurrentUser();

        setSize(400, 420);
        setLocationRelativeTo(parent);
        setResizable(false);

        initUI();
    }

    private void initUI() {
        JPanel panel = UIComponentFactory.createRoundedPanel(UIComponentFactory.COLOR_BACKGROUND, 0);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JLabel titleLbl = new JLabel("User Profile Management");
        titleLbl.setFont(UIComponentFactory.FONT_HEADER);
        titleLbl.setForeground(UIComponentFactory.COLOR_TEXT);
        panel.add(titleLbl);
        panel.add(Box.createVerticalStrut(15));

        JLabel uLbl = new JLabel("Username: " + currentUser.getUsername() + " | Email: " + currentUser.getEmail());
        uLbl.setFont(UIComponentFactory.FONT_SMALL);
        uLbl.setForeground(UIComponentFactory.COLOR_TEXT_MUTED);
        panel.add(uLbl);
        panel.add(Box.createVerticalStrut(15));

        JLabel bioLbl = new JLabel("Status / Bio");
        bioLbl.setFont(UIComponentFactory.FONT_SMALL);
        bioLbl.setForeground(UIComponentFactory.COLOR_TEXT);
        panel.add(bioLbl);
        panel.add(Box.createVerticalStrut(4));

        bioField = UIComponentFactory.createStyledTextField(currentUser.getBio() != null ? currentUser.getBio() : "");
        bioField.setText(currentUser.getBio() != null ? currentUser.getBio() : "");
        panel.add(bioField);
        panel.add(Box.createVerticalStrut(15));

        JLabel passHeader = new JLabel("Change Password (Optional)");
        passHeader.setFont(UIComponentFactory.FONT_HEADER);
        passHeader.setForeground(UIComponentFactory.COLOR_TEXT);
        panel.add(passHeader);
        panel.add(Box.createVerticalStrut(8));

        currentPassField = UIComponentFactory.createStyledPasswordField();
        panel.add(new JLabel("Current Password:"));
        panel.add(currentPassField);
        panel.add(Box.createVerticalStrut(6));

        newPassField = UIComponentFactory.createStyledPasswordField();
        panel.add(new JLabel("New Password:"));
        panel.add(newPassField);
        panel.add(Box.createVerticalStrut(20));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);

        JButton saveBtn = UIComponentFactory.createStyledButton("Save Changes", UIComponentFactory.COLOR_PRIMARY, UIComponentFactory.COLOR_BACKGROUND);
        saveBtn.addActionListener(e -> saveProfile());
        btnPanel.add(saveBtn);

        panel.add(btnPanel);

        add(panel);
    }

    private void saveProfile() {
        String bio = bioField.getText().trim();
        String currentPass = new String(currentPassField.getPassword());
        String newPass = new String(newPassField.getPassword());

        String cp = currentPass.isEmpty() ? null : currentPass;
        String np = newPass.isEmpty() ? null : newPass;

        client.updateProfile(bio, null, cp, np);
        JOptionPane.showMessageDialog(this, "Profile update requested.", "Info", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }
}

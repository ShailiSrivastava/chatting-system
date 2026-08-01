package com.chat.client.ui;

import com.chat.client.network.ChatClient;
import com.chat.common.model.User;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateGroupDialog extends JDialog {

    private final ChatClient client;
    private final List<User> availableUsers;

    private JTextField nameField;
    private JTextField descField;
    private final Map<Long, JCheckBox> userCheckBoxes = new HashMap<>();

    public CreateGroupDialog(Frame parent, ChatClient client, List<User> availableUsers) {
        super(parent, "Create New Chat Group", true);
        this.client = client;
        this.availableUsers = availableUsers;

        setSize(400, 480);
        setLocationRelativeTo(parent);
        setResizable(false);

        initUI();
    }

    private void initUI() {
        JPanel panel = UIComponentFactory.createRoundedPanel(UIComponentFactory.COLOR_BACKGROUND, 0);
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Form header/inputs
        JPanel formPanel = new JPanel();
        formPanel.setOpaque(false);
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));

        JLabel titleLbl = new JLabel("Group Details");
        titleLbl.setFont(UIComponentFactory.FONT_HEADER);
        titleLbl.setForeground(UIComponentFactory.COLOR_TEXT);
        formPanel.add(titleLbl);
        formPanel.add(Box.createVerticalStrut(8));

        nameField = UIComponentFactory.createStyledTextField("Group Name");
        formPanel.add(nameField);
        formPanel.add(Box.createVerticalStrut(8));

        descField = UIComponentFactory.createStyledTextField("Description (optional)");
        formPanel.add(descField);
        formPanel.add(Box.createVerticalStrut(12));

        JLabel membersLbl = new JLabel("Select Members");
        membersLbl.setFont(UIComponentFactory.FONT_HEADER);
        membersLbl.setForeground(UIComponentFactory.COLOR_TEXT);
        formPanel.add(membersLbl);
        formPanel.add(Box.createVerticalStrut(8));

        panel.add(formPanel, BorderLayout.NORTH);

        // Members Checklist
        JPanel membersPanel = new JPanel();
        membersPanel.setLayout(new BoxLayout(membersPanel, BoxLayout.Y_AXIS));
        membersPanel.setBackground(UIComponentFactory.COLOR_SURFACE);

        for (User u : availableUsers) {
            if (u.getId().equals(client.getCurrentUser().getId())) continue; // Skip current user
            JCheckBox cb = new JCheckBox(u.getUsername() + " (" + u.getEmail() + ")");
            cb.setFont(UIComponentFactory.FONT_MAIN);
            cb.setForeground(UIComponentFactory.COLOR_TEXT);
            cb.setBackground(UIComponentFactory.COLOR_SURFACE);
            cb.setFocusPainted(false);
            userCheckBoxes.put(u.getId(), cb);
            membersPanel.add(cb);
        }

        JScrollPane scrollPane = new JScrollPane(membersPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UIComponentFactory.COLOR_SURFACE);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Bottom Actions
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);

        JButton cancelBtn = UIComponentFactory.createStyledButton("Cancel", UIComponentFactory.COLOR_SURFACE_LIGHT, UIComponentFactory.COLOR_TEXT);
        cancelBtn.addActionListener(e -> dispose());
        btnPanel.add(cancelBtn);

        JButton createBtn = UIComponentFactory.createStyledButton("Create Group", UIComponentFactory.COLOR_PRIMARY, UIComponentFactory.COLOR_BACKGROUND);
        createBtn.addActionListener(e -> handleCreateGroup());
        btnPanel.add(createBtn);

        panel.add(btnPanel, BorderLayout.SOUTH);

        add(panel);
    }

    private void handleCreateGroup() {
        String name = nameField.getText().trim();
        String desc = descField.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a group name.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<Long> selectedMemberIds = new ArrayList<>();
        for (Map.Entry<Long, JCheckBox> entry : userCheckBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selectedMemberIds.add(entry.getKey());
            }
        }

        client.createGroup(name, desc, selectedMemberIds);
        dispose();
    }
}

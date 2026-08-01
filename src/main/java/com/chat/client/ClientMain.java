package com.chat.client;

import com.chat.client.network.ChatClient;
import com.chat.client.ui.LoginFrame;

import javax.swing.*;

public class ClientMain {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Default fallback
            }

            ChatClient client = new ChatClient();
            LoginFrame loginFrame = new LoginFrame(client);
            loginFrame.setVisible(true);
        });
    }
}

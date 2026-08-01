package com.chat.client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class UIComponentFactory {

    // Theme Color Palette
    public static final Color COLOR_BACKGROUND = new Color(30, 30, 46);       // Deep Slate
    public static final Color COLOR_SIDEBAR = new Color(24, 24, 37);         // Darker Slate
    public static final Color COLOR_SURFACE = new Color(49, 50, 68);         // Panel background
    public static final Color COLOR_SURFACE_LIGHT = new Color(69, 71, 90);   // Hover/Secondary Surface
    public static final Color COLOR_PRIMARY = new Color(137, 180, 250);      // Soft Accent Blue
    public static final Color COLOR_PRIMARY_HOVER = new Color(116, 199, 236);
    public static final Color COLOR_ONLINE = new Color(166, 227, 161);       // Emerald Green
    public static final Color COLOR_OFFLINE = new Color(147, 153, 178);      // Muted Gray
    public static final Color COLOR_TEXT = new Color(205, 214, 244);         // Main Text
    public static final Color COLOR_TEXT_MUTED = new Color(166, 173, 200);   // Subtext
    public static final Color COLOR_SENT_BUBBLE = new Color(40, 110, 210);    // Sent Message Blue
    public static final Color COLOR_RECV_BUBBLE = new Color(49, 50, 68);     // Received Message Slate

    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_MAIN = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);

    public static JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(bg.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(bg.brighter());
                } else {
                    g2.setColor(bg);
                }
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_HEADER);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(140, 40));
        return btn;
    }

    public static JTextField createStyledTextField(String placeholder) {
        JTextField tf = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_SURFACE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tf.setFont(FONT_MAIN);
        tf.setForeground(COLOR_TEXT);
        tf.setCaretColor(COLOR_PRIMARY);
        tf.setOpaque(false);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 0, 0, 0),
                new EmptyBorder(8, 12, 8, 12)
        ));
        return tf;
    }

    public static JPasswordField createStyledPasswordField() {
        JPasswordField pf = new JPasswordField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_SURFACE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pf.setFont(FONT_MAIN);
        pf.setForeground(COLOR_TEXT);
        pf.setCaretColor(COLOR_PRIMARY);
        pf.setOpaque(false);
        pf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 0, 0, 0),
                new EmptyBorder(8, 12, 8, 12)
        ));
        return pf;
    }

    public static JPanel createRoundedPanel(Color bg, int radius) {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        return panel;
    }
}

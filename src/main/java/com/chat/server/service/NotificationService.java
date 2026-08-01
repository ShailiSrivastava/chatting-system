package com.chat.server.service;

import com.chat.common.model.Notification;
import com.chat.server.dao.NotificationDAO;
import com.chat.server.dao.impl.NotificationDAOImpl;

import java.util.List;

public class NotificationService {

    private final NotificationDAO notificationDAO;

    public NotificationService() {
        this.notificationDAO = new NotificationDAOImpl();
    }

    public Notification sendNotification(Long userId, String title, String content) {
        Notification notification = new Notification(userId, title, content);
        if (notificationDAO.createNotification(notification)) {
            return notification;
        }
        return null;
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationDAO.getUserNotifications(userId);
    }

    public boolean markAsRead(Long notificationId) {
        return notificationDAO.markAsRead(notificationId);
    }
}

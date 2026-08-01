package com.chat.server.dao;

import com.chat.common.model.Notification;

import java.util.List;

public interface NotificationDAO {
    boolean createNotification(Notification notification);
    List<Notification> getUserNotifications(Long userId);
    boolean markAsRead(Long notificationId);
}

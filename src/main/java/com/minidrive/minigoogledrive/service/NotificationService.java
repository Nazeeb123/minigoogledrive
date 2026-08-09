
package com.minidrive.minigoogledrive.service;

import com.minidrive.minigoogledrive.model.Notification;
import com.minidrive.minigoogledrive.model.User;
import com.minidrive.minigoogledrive.repository.NotificationRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import java.util.List;
@Service

public class NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationService(
            
            NotificationRepository notificationRepository) {
        this.notificationRepository =
                notificationRepository;
    }


    // Create notification
    public void createNotification(
            User user,
            String message) {

        Notification notification =
                new Notification();

        notification.setUser(user);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setCreatedAt(
                LocalDateTime.now()
        );

        notificationRepository.save(notification);
    }


    // Get all notifications
    public List<Notification> getUserNotifications(
            User user) {

        return notificationRepository
                .findByUserOrderByCreatedAtDesc(user);
    }


    // Get unread count
    public long getUnreadCount(User user) {

        return notificationRepository
                .countByUserAndReadFalse(user);
    }


    // Mark notification as read
    public void markAsRead(Long id) {

        Notification notification =
                notificationRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Notification not found"
                                )
                        );

        notification.setRead(true);

        notificationRepository.save(notification);
    }
}



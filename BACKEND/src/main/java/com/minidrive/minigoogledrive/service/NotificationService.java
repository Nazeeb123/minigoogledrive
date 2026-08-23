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

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // CREATE NOTIFICATION
    public Notification createNotification(User user, String message) {

        Notification notification = new Notification();

        notification.setUser(user);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        return notificationRepository.save(notification);
    }

    // GET CURRENT USER'S NOTIFICATIONS
    public List<Notification> getUserNotifications(User user) {

        return notificationRepository
                .findByUserOrderByCreatedAtDesc(user);
    }

    // UNREAD COUNT
    public long getUnreadCount(User user) {

        return notificationRepository
                .countByUserAndReadFalse(user);
    }

    // MARK AS READ
    public void markAsRead(Long id, User currentUser) {

        Notification notification = notificationRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Notification not found"));

        // SECURITY CHECK
        if (notification.getUser() == null ||
                !notification.getUser().getId().equals(currentUser.getId())) {

            throw new RuntimeException(
                    "You cannot modify this notification");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    // DELETE
    public void deleteNotification(Long id, User currentUser) {

        Notification notification = notificationRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Notification not found"));

        // SECURITY CHECK
        if (notification.getUser() == null ||
                !notification.getUser().getId().equals(currentUser.getId())) {

            throw new RuntimeException(
                    "You cannot delete this notification");
        }

        notificationRepository.delete(notification);
    }
}
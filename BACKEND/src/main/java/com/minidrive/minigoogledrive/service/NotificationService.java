
package com.minidrive.minigoogledrive.service;

import com.minidrive.minigoogledrive.model.Notification;
import com.minidrive.minigoogledrive.model.User;
import com.minidrive.minigoogledrive.repository.NotificationRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import java.util.List;

@Service

public class NotificationService {
        @Autowired
        private NotificationRepository notificationRepository;

        public NotificationService(

                        NotificationRepository notificationRepository) {
                this.notificationRepository = notificationRepository;
        }

        // Create notification
        public void createNotification(
                        User user,
                        String message) {

                Notification notification = new Notification();

                notification.setUser(user);
                notification.setMessage(message);
                notification.setRead(false);
                notification.setCreatedAt(
                                LocalDateTime.now());

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
        public void markAsRead(
                        Long id,
                        User currentUser) {

                Notification notification = notificationRepository
                                .findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Notification not found"));

                if (!notification.getUser().getId()
                                .equals(currentUser.getId())) {

                        throw new RuntimeException(
                                        "You cannot modify this notification");
                }

                notification.setRead(true);

                notificationRepository.save(notification);
        }

        public void deleteNotification(Long id) {

                Notification notification = notificationRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Notification not found"));

                notificationRepository.delete(notification);
        }
}

package com.minidrive.minigoogledrive.controller;

import com.minidrive.minigoogledrive.model.Notification;
import com.minidrive.minigoogledrive.model.User;
import com.minidrive.minigoogledrive.repository.UserRepository;
import com.minidrive.minigoogledrive.service.NotificationService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(
            NotificationService notificationService,
            UserRepository userRepository) {

        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {

        var authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getName())) {

            throw new RuntimeException("User is not authenticated");
        }

        String email = authentication.getName();

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found: " + email));
    }

    // GET NOTIFICATIONS
    @GetMapping
    public List<Notification> getNotifications() {

        User user = getCurrentUser();

        return notificationService
                .getUserNotifications(user);
    }

    // UNREAD COUNT
    @GetMapping("/unread-count")
    public long getUnreadCount() {

        User user = getCurrentUser();

        return notificationService
                .getUnreadCount(user);
    }

    // MARK AS READ
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long id) {

        User user = getCurrentUser();

        notificationService.markAsRead(id, user);

        return ResponseEntity.ok(
                "Notification marked as read");
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(
            @PathVariable Long id) {

        User user = getCurrentUser();

        notificationService.deleteNotification(
                id,
                user);

        return ResponseEntity.ok(
                "Notification deleted successfully");
    }
}
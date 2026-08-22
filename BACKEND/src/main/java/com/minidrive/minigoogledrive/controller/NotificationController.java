package com.minidrive.minigoogledrive.controller;

import com.minidrive.minigoogledrive.model.Notification;
import com.minidrive.minigoogledrive.model.User;
import com.minidrive.minigoogledrive.repository.UserRepository;
import com.minidrive.minigoogledrive.service.NotificationService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;


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

        // =========================
        // GET CURRENT USER
        // =========================

        private User getCurrentUser() {

                var authentication = SecurityContextHolder
                                .getContext()
                                .getAuthentication();

                System.out.println("===== NOTIFICATION AUTH =====");

                System.out.println(
                                "Authentication: " + authentication);

                if (authentication == null) {

                        System.out.println(
                                        "AUTHENTICATION IS NULL");

                        throw new RuntimeException(
                                        "Authentication is null");
                }

                System.out.println(
                                "Authenticated: "
                                                + authentication.isAuthenticated());

                System.out.println(
                                "Username: "
                                                + authentication.getName());

                String email = authentication.getName();

                User user = userRepository
                                .findByEmail(email)
                                .orElseThrow(() -> new RuntimeException(
                                                "User not found: "
                                                                + email));

                System.out.println(
                                "USER FOUND: "
                                                + user.getEmail());

                return user;
        }

        // =========================
        // GET NOTIFICATIONS
        // =========================

        @GetMapping
        public List<Notification> getNotifications() {

                System.out.println(
                                "GET /notifications");

                User user = getCurrentUser();

                System.out.println(
                                "Getting notifications for: "
                                                + user.getEmail());

                return notificationService
                                .getUserNotifications(user);
        }

        // =========================
        // GET UNREAD COUNT
        // =========================

        @GetMapping("/unread-count")
        public long getUnreadCount() {

                System.out.println(
                                "GET /notifications/unread-count");

                User user = getCurrentUser();

                System.out.println(
                                "Getting unread count for: "
                                                + user.getEmail());

                return notificationService
                                .getUnreadCount(user);
        }

        // =========================
        // MARK AS READ
        // =========================

        @PutMapping("/{id}/read")
        public String markAsRead(
                        @PathVariable Long id) {

                User user = getCurrentUser();

                notificationService
                                .markAsRead(id, user);

                return "Notification marked as read";
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<?> deleteNotification(@PathVariable Long id) {

                notificationService.deleteNotification(id);

                return ResponseEntity.ok("Notification deleted successfully");
        }
}
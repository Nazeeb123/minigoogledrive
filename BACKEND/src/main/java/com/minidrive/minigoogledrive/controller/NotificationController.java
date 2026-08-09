package com.minidrive.minigoogledrive.controller;

import com.minidrive.minigoogledrive.model.Notification;
import com.minidrive.minigoogledrive.model.User;
import com.minidrive.minigoogledrive.repository.UserRepository;
import com.minidrive.minigoogledrive.service.NotificationService;

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

        this.notificationService =
                notificationService;

        this.userRepository =
                userRepository;
    }


    private User getCurrentUser() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );
    }


    // Get notifications
    @GetMapping
    public List<Notification> getNotifications() {

        return notificationService
                .getUserNotifications(
                        getCurrentUser()
                );
    }


    // Get unread count
    @GetMapping("/unread-count")
    public long getUnreadCount() {

        return notificationService
                .getUnreadCount(
                        getCurrentUser()
                );
    }


    // Mark notification as read
    @PutMapping("/{id}/read")
    public String markAsRead(
            @PathVariable Long id) {

        notificationService
                .markAsRead(id);

        return "Notification marked as read";
    }
}





package com.minidrive.minigoogledrive.repository;

import com.minidrive.minigoogledrive.model.Notification;
import com.minidrive.minigoogledrive.model.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    long countByUserAndReadFalse(User user);
}


package com.minidrive.minigoogledrive.repository;

import com.minidrive.minigoogledrive.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    List<Chat> findByUserEmailOrderByUpdatedAtDesc(String userEmail);

    Optional<Chat> findByIdAndUserEmail(Long id, String userEmail);
}
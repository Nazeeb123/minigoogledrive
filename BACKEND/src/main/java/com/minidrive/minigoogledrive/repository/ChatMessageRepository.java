package com.minidrive.minigoogledrive.repository;

import com.minidrive.minigoogledrive.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatIdOrderByCreatedAtAsc(Long chatId);

    void deleteByChatId(Long chatId);
}
package com.minidrive.minigoogledrive.service;

import com.minidrive.minigoogledrive.model.Chat;
import com.minidrive.minigoogledrive.model.ChatMessage;
import com.minidrive.minigoogledrive.repository.ChatMessageRepository;
import com.minidrive.minigoogledrive.repository.ChatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final OllamaService ollamaService;

    public ChatService(
            ChatRepository chatRepository,
            ChatMessageRepository chatMessageRepository,
            OllamaService ollamaService) {
        this.chatRepository = chatRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.ollamaService = ollamaService;
    }

    // Create a new chat
    public Chat createChat(String userEmail, String title) {

        if (title == null || title.trim().isEmpty()) {
            title = "New Chat";
        }

        Chat chat = new Chat(userEmail, title);

        return chatRepository.save(chat);
    }

    // Get all chats of logged-in user
    public List<Chat> getUserChats(String userEmail) {

        return chatRepository.findByUserEmailOrderByUpdatedAtDesc(userEmail);
    }

    // Get messages of one chat
    public List<ChatMessage> getMessages(
            Long chatId,
            String userEmail) {

        Chat chat = chatRepository
                .findByIdAndUserEmail(chatId, userEmail)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        return chatMessageRepository
                .findByChatIdOrderByCreatedAtAsc(chat.getId());
    }

    // Ask AI and save conversation
    public ChatMessage askAI(
            Long chatId,
            String userEmail,
            String question) {

        Chat chat = chatRepository
                .findByIdAndUserEmail(chatId, userEmail)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        if (question == null || question.trim().isEmpty()) {
            throw new RuntimeException("Question cannot be empty");
        }

        // Ask Ollama
        String answer = ollamaService.askAI(question);

        // Save question + answer
        ChatMessage message = new ChatMessage(chat, question, answer);

        ChatMessage savedMessage = chatMessageRepository.save(message);

        // Update chat timestamp
        chat.setUpdatedAt(LocalDateTime.now());

        chatRepository.save(chat);

        return savedMessage;
    }

    // Ask AI about a file and save conversation
    public ChatMessage askAboutFile(
            Long chatId,
            String userEmail,
            String question,
            String fileName,
            String content) {

        Chat chat = chatRepository
                .findByIdAndUserEmail(chatId, userEmail)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        if (question == null || question.trim().isEmpty()) {
            throw new RuntimeException("Question cannot be empty");
        }

        // Ask Ollama about the file
        String answer = ollamaService.askAboutFile(
                question,
                fileName,
                content);

        // Save question + answer
        ChatMessage message = new ChatMessage(
                chat,
                question,
                answer);

        ChatMessage savedMessage = chatMessageRepository.save(message);

        // Update chat timestamp
        chat.setUpdatedAt(LocalDateTime.now());

        chatRepository.save(chat);

        return savedMessage;
    }

    // Delete complete chat
    @Transactional
    public void deleteChat(
            Long chatId,
            String userEmail) {

        Chat chat = chatRepository
                .findByIdAndUserEmail(chatId, userEmail)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        chatMessageRepository.deleteByChatId(chat.getId());

        chatRepository.delete(chat);
    }

    // Rename chat
    public Chat renameChat(
            Long chatId,
            String userEmail,
            String title) {

        Chat chat = chatRepository
                .findByIdAndUserEmail(chatId, userEmail)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        chat.setTitle(title);
        chat.setUpdatedAt(LocalDateTime.now());

        return chatRepository.save(chat);
    }
}

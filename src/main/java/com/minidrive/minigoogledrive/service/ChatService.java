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
        private final OpenRouterService openRouterService;
        private final MemoryService memoryService;

        public ChatService(
                        ChatRepository chatRepository,
                        ChatMessageRepository chatMessageRepository,
                        OpenRouterService openRouterService,
                        MemoryService memoryService) {

                this.chatRepository = chatRepository;
                this.chatMessageRepository = chatMessageRepository;
                this.openRouterService = openRouterService;
                this.memoryService = memoryService;
        }

        // =========================================================
        // CREATE CHAT
        // =========================================================

        public Chat createChat(
                        String userEmail,
                        String title) {

                if (title == null || title.trim().isEmpty()) {
                        title = "New Chat";
                }

                Chat chat = new Chat(
                                userEmail,
                                title);

                return chatRepository.save(chat);
        }

        // =========================================================
        // GET USER CHATS
        // =========================================================

        public List<Chat> getUserChats(
                        String userEmail) {

                return chatRepository
                                .findByUserEmailOrderByUpdatedAtDesc(userEmail);
        }

        // =========================================================
        // GET CHAT MESSAGES
        // =========================================================

        public List<ChatMessage> getMessages(
                        Long chatId,
                        String userEmail) {

                Chat chat = chatRepository
                                .findByIdAndUserEmail(
                                                chatId,
                                                userEmail)
                                .orElseThrow(() -> new RuntimeException("Chat not found"));

                return chatMessageRepository
                                .findByChatIdOrderByCreatedAtAsc(
                                                chat.getId());
        }

        // =========================================================
        // NORMAL AI CHAT
        // =========================================================

        public ChatMessage askAI(
                        Long chatId,
                        String userEmail,
                        String question) {

                Chat chat = chatRepository
                                .findByIdAndUserEmail(
                                                chatId,
                                                userEmail)
                                .orElseThrow(() -> new RuntimeException("Chat not found"));

                if (question == null ||
                                question.trim().isEmpty()) {

                        throw new RuntimeException(
                                        "Question cannot be empty");
                }

                // =====================================================
                // GET PREVIOUS CONVERSATION
                // =====================================================

                List<ChatMessage> previousMessages = chatMessageRepository
                                .findByChatIdOrderByCreatedAtAsc(
                                                chat.getId());

                // =====================================================
                // BUILD CONVERSATION
                // =====================================================

                StringBuilder conversation = new StringBuilder();

                String memoryContext = memoryService.buildMemoryContext(userEmail);

                if (!memoryContext.isEmpty()) {

                        conversation.append(memoryContext);
                        conversation.append("\n");
                }

                conversation.append("""
                                You are continuing a conversation with the user.

                                Remember the previous conversation and use it
                                when answering the new question.

                                Previous conversation:
                                """);

                for (ChatMessage message : previousMessages) {

                        conversation
                                        .append("\nUser: ")
                                        .append(message.getQuestion());

                        conversation
                                        .append("\nAssistant: ")
                                        .append(message.getAnswer());

                        conversation.append("\n");
                }

                conversation.append("\nCurrent user question:\n");
                conversation.append(question);

                // =====================================================
                // ASK OPENROUTER
                // =====================================================

                String answer = openRouterService.askAI(
                                conversation.toString());

                // =====================================================
                // SAVE MESSAGE
                // =====================================================

                ChatMessage message = new ChatMessage(
                                chat,
                                question,
                                answer);

                ChatMessage savedMessage = chatMessageRepository.save(message);
                // =====================================================
                // SAVE USEFUL USER MEMORY
                // =====================================================

                try {

                        memoryService.saveMemoryIfUseful(
                                        userEmail,
                                        question);

                } catch (Exception e) {

                        // Memory failure should NEVER break the chat
                        System.out.println(
                                        "MEMORY SAVE FAILED: "
                                                        + e.getMessage());
                }

                // =====================================================
                // UPDATE CHAT
                // =====================================================

                chat.setUpdatedAt(
                                LocalDateTime.now());

                chatRepository.save(chat);

                return savedMessage;
        }

        // =========================================================
        // FILE CHAT
        // =========================================================

        public ChatMessage askAboutFile(
                        Long chatId,
                        String userEmail,
                        String question,
                        String fileName,
                        String content) {

                Chat chat = chatRepository
                                .findByIdAndUserEmail(
                                                chatId,
                                                userEmail)
                                .orElseThrow(() -> new RuntimeException("Chat not found"));

                if (question == null ||
                                question.trim().isEmpty()) {

                        throw new RuntimeException(
                                        "Question cannot be empty");
                }

                String answer = openRouterService.askAboutFile(
                                question,
                                fileName,
                                content);

                ChatMessage message = new ChatMessage(
                                chat,
                                question,
                                answer);

                ChatMessage savedMessage = chatMessageRepository.save(message);

                chat.setUpdatedAt(
                                LocalDateTime.now());

                chatRepository.save(chat);

                return savedMessage;
        }

        // =========================================================
        // DELETE CHAT
        // =========================================================

        @Transactional
        public void deleteChat(
                        Long chatId,
                        String userEmail) {

                Chat chat = chatRepository
                                .findByIdAndUserEmail(
                                                chatId,
                                                userEmail)
                                .orElseThrow(() -> new RuntimeException("Chat not found"));

                chatMessageRepository
                                .deleteByChatId(chat.getId());

                chatRepository.delete(chat);
        }

        // =========================================================
        // RENAME CHAT
        // =========================================================

        public Chat renameChat(
                        Long chatId,
                        String userEmail,
                        String title) {

                Chat chat = chatRepository
                                .findByIdAndUserEmail(
                                                chatId,
                                                userEmail)
                                .orElseThrow(() -> new RuntimeException("Chat not found"));

                chat.setTitle(title);
                chat.setUpdatedAt(
                                LocalDateTime.now());

                return chatRepository.save(chat);
        }
}
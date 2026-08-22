package com.minidrive.minigoogledrive.controller;

import com.minidrive.minigoogledrive.model.Chat;
import com.minidrive.minigoogledrive.model.ChatMessage;
import com.minidrive.minigoogledrive.service.ChatService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai/chats")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    private String getCurrentUserEmail() {

        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
    }

    // CREATE CHAT
    @PostMapping
    public Chat createChat(
            @RequestBody Map<String, String> request
    ) {

        String email = getCurrentUserEmail();

        String title = request.get("title");

        return chatService.createChat(email, title);
    }

    // GET ALL CHATS
    @GetMapping
    public List<Chat> getChats() {

        String email = getCurrentUserEmail();

        return chatService.getUserChats(email);
    }

    // GET CHAT MESSAGES
    @GetMapping("/{chatId}/messages")
    public List<ChatMessage> getMessages(
            @PathVariable Long chatId
    ) {

        String email = getCurrentUserEmail();

        return chatService.getMessages(chatId, email);
    }

    // ASK AI
    @PostMapping("/{chatId}/ask")
    public ChatMessage askAI(
            @PathVariable Long chatId,
            @RequestBody Map<String, String> request
    ) {

        String email = getCurrentUserEmail();

        String question = request.get("question");

        return chatService.askAI(
                chatId,
                email,
                question
        );
    }

    // RENAME CHAT
    @PutMapping("/{chatId}")
    public Chat renameChat(
            @PathVariable Long chatId,
            @RequestBody Map<String, String> request
    ) {

        String email = getCurrentUserEmail();

        String title = request.get("title");

        return chatService.renameChat(
                chatId,
                email,
                title
        );
    }

    // DELETE CHAT
    @DeleteMapping("/{chatId}")
    public String deleteChat(
            @PathVariable Long chatId
    ) {

        String email = getCurrentUserEmail();

        chatService.deleteChat(
                chatId,
                email
        );

        return "Chat deleted successfully";
    }
}

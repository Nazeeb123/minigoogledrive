package com.minidrive.minigoogledrive.service;

import com.minidrive.minigoogledrive.model.UserMemory;
import com.minidrive.minigoogledrive.repository.UserMemoryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MemoryService {

    private final UserMemoryRepository memoryRepository;
    private final OpenRouterService openRouterService;

    public MemoryService(
            UserMemoryRepository memoryRepository,
            OpenRouterService openRouterService) {

        this.memoryRepository = memoryRepository;
        this.openRouterService = openRouterService;
    }

    // =========================================================
    // GET USER MEMORIES
    // =========================================================

    public List<UserMemory> getMemories(String userEmail) {

        return memoryRepository
                .findByUserEmailOrderByUpdatedAtDesc(userEmail);
    }

    // =========================================================
    // ADD MEMORY
    // =========================================================

    public UserMemory addMemory(
            String userEmail,
            String memory) {

        if (userEmail == null ||
                userEmail.trim().isEmpty()) {
            throw new RuntimeException("User email is required");
        }

        if (memory == null ||
                memory.trim().isEmpty()) {
            return null;
        }

        UserMemory userMemory = new UserMemory(
                userEmail,
                memory.trim());

        return memoryRepository.save(userMemory);
    }

    // =========================================================
    // DELETE ALL MEMORIES
    // =========================================================

    @Transactional
    public void clearMemories(String userEmail) {

        memoryRepository.deleteByUserEmail(userEmail);
    }

    // =========================================================
    // BUILD MEMORY CONTEXT
    // =========================================================

    public String buildMemoryContext(String userEmail) {

        List<UserMemory> memories = getMemories(userEmail);

        if (memories.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();

        context.append("""
                USER MEMORY

                The following are facts/preferences previously
                provided or established by the user.

                Use them only when relevant.
                Do not mention that you have memory unless useful.
                Do not invent additional facts.

                """);

        for (UserMemory memory : memories) {

            context.append("- ")
                    .append(memory.getMemory())
                    .append("\n");
        }

        return context.toString();
    }

    public void saveMemoryIfUseful(
            String userEmail,
            String question) {

        if (question == null || question.trim().isEmpty()) {
            return;
        }

        try {

            String prompt = """
                    Analyze the user's message and determine whether it
                    contains useful long-term information that an AI assistant
                    should remember about the user.

                    Remember things such as:
                    - Preferences
                    - Learning goals
                    - Career goals
                    - Programming preferences
                    - Communication preferences
                    - Long-term projects
                    - Frequently mentioned interests
                    - Explicit personal facts that are useful for future assistance

                    Do NOT remember:
                    - Temporary questions
                    - One-time tasks
                    - Random statements
                    - Passwords
                    - API keys
                    - Financial credentials
                    - Sensitive private information
                    - Medical information
                    - Political or religious information

                    If there is something useful to remember, return ONLY the
                    memory as one short sentence.

                    If there is nothing worth remembering, return exactly:

                    NO_MEMORY

                    USER MESSAGE:
                    """ + question;

            String result = openRouterService.askAI(prompt);

            if (result == null) {
                return;
            }

            result = result.trim();

            if (result.equalsIgnoreCase("NO_MEMORY")) {
                return;
            }

            if (result.length() < 5 ||
                    result.length() > 500) {
                return;
            }

            addMemory(
                    userEmail,
                    result);

            System.out.println(
                    "🧠 MEMORY SAVED: "
                            + result);

        } catch (Exception e) {

            // Memory must NEVER break normal chat.
            System.out.println(
                    "MEMORY EXTRACTION FAILED: "
                            + e.getMessage());
        }
    }
}
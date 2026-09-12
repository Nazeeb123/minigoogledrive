package com.minidrive.minigoogledrive.controller;

import com.minidrive.minigoogledrive.model.FileData;
import com.minidrive.minigoogledrive.service.ChatService;
import com.minidrive.minigoogledrive.service.CloudinaryService;
import com.minidrive.minigoogledrive.service.FileDataService;
import com.minidrive.minigoogledrive.service.FileTextService;
import com.minidrive.minigoogledrive.service.OpenAIService;
import com.minidrive.minigoogledrive.service.OpenRouterService;
import com.minidrive.minigoogledrive.service.RagService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.nio.file.Path;
import java.util.HashMap;

@RestController
@RequestMapping("/ai")
public class AIController {

        private final OpenAIService openAIService;
        private final FileTextService fileTextService;
        private final FileDataService fileDataService;
        private final ChatService chatService;
        private final OpenRouterService openRouterService;
        private final RagService ragService;
        private final CloudinaryService cloudinaryService;

        public AIController(
                        OpenAIService openAIService,
                        FileDataService fileDataService,
                        ChatService chatService,
                        OpenRouterService openRouterService,
                        FileTextService fileTextService,
                        RagService ragService,
                        CloudinaryService cloudinaryService) {

                this.openAIService = openAIService;
                this.fileDataService = fileDataService;
                this.chatService = chatService;
                this.openRouterService = openRouterService;
                this.fileTextService = fileTextService;
                this.ragService = ragService;
                this.cloudinaryService = cloudinaryService;
        }

        // =========================================================
        // NORMAL AI
        // =========================================================

        @PostMapping("/ask")
        public String askAI(
                        @RequestBody Map<String, String> request) {

                String question = request.get("question");

                if (question == null || question.trim().isEmpty()) {
                        throw new RuntimeException(
                                        "Question cannot be empty");
                }

                return openRouterService.askAI(question);
        }

        // =========================================================
        // CHAT WITH FILE
        // PDF / DOC / DOCX / TXT
        // =========================================================

        @PostMapping("/file-ask")
        public Map<String, String> askAboutFile(
                        @RequestBody Map<String, Object> request) {

                long startTime = System.currentTimeMillis();

                try {

                        // -----------------------------------------------------
                        // QUESTION
                        // -----------------------------------------------------

                        Object questionObject = request.get("question");

                        if (questionObject == null) {
                                throw new RuntimeException(
                                                "Question is missing");
                        }

                        String question = String.valueOf(questionObject).trim();

                        if (question.isEmpty()) {
                                throw new RuntimeException(
                                                "Question cannot be empty");
                        }

                        // -----------------------------------------------------
                        // FILE ID
                        // -----------------------------------------------------

                        Object fileIdObject = request.get("fileId");

                        if (fileIdObject == null) {
                                throw new RuntimeException(
                                                "fileId is missing");
                        }

                        Long fileId = Long.parseLong(
                                        String.valueOf(fileIdObject));

                        // -----------------------------------------------------
                        // GET FILE
                        // -----------------------------------------------------

                        FileData fileData = fileDataService.getFileForAI(fileId);

                        if (fileData == null) {
                                throw new RuntimeException(
                                                "File not found");
                        }

                        System.out.println(
                                        "=================================");

                        System.out.println(
                                        "RAG FILE ASK");

                        System.out.println(
                                        "FILE: "
                                                        + fileData.getFileName());

                        System.out.println(
                                        "QUESTION: "
                                                        + question);

                        System.out.println(
                                        "=================================");

                        // -----------------------------------------------------
                        // IMAGE PROTECTION
                        // -----------------------------------------------------

                        /*
                         * Images MUST NOT go through normal RAG.
                         *
                         * Image questions are handled by /image-ask.
                         *
                         * This prevents OCR/RAG from incorrectly treating
                         * a gaming logo or normal photograph as a document.
                         */

                        String fileType = fileData.getFileType();

                        if (fileType != null &&
                                        fileType.startsWith("image/")) {

                                throw new RuntimeException(
                                                "This is an image. Please use the image analysis endpoint.");
                        }

                        // -----------------------------------------------------
                        // RAG
                        // -----------------------------------------------------

                        String answer = ragService.askAboutFile(
                                        fileData,
                                        question);

                        long elapsed = System.currentTimeMillis()
                                        - startTime;

                        System.out.println(
                                        "RAG RESPONSE TIME: "
                                                        + elapsed
                                                        + " ms");

                        return Map.of(
                                        "answer",
                                        answer);

                } catch (Exception e) {

                        e.printStackTrace();

                        throw new RuntimeException(
                                        "Could not process RAG question: "
                                                        + e.getMessage(),
                                        e);
                }
        }

        // =========================================================
        // IMAGE ANALYSIS
        // JPG / JPEG / PNG
        // =========================================================

        @PostMapping(value = "/image-ask", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public Map<String, String> askAboutImage(
                        @RequestParam("fileId") Long fileId,
                        @RequestParam("question") String question) {

                long startTime = System.currentTimeMillis();

                try {

                        // -------------------------------------------------
                        // VALIDATE QUESTION
                        // -------------------------------------------------

                        if (question == null ||
                                        question.trim().isEmpty()) {

                                throw new RuntimeException(
                                                "Question cannot be empty");
                        }

                        // -------------------------------------------------
                        // GET FILE
                        // -------------------------------------------------

                        FileData fileData = fileDataService.getFileForAI(fileId);

                        if (fileData == null) {

                                throw new RuntimeException(
                                                "Image file not found");
                        }

                        System.out.println(
                                        "=================================");

                        System.out.println(
                                        "IMAGE AI");

                        System.out.println(
                                        "FILE: "
                                                        + fileData.getFileName());

                        System.out.println(
                                        "QUESTION: "
                                                        + question);

                        System.out.println(
                                        "=================================");

                        // -------------------------------------------------
                        // FILE TYPE
                        // -------------------------------------------------

                        String mimeType = fileData.getFileType();

                        if (mimeType == null ||
                                        !mimeType.startsWith("image/")) {

                                throw new RuntimeException(
                                                "Selected file is not an image");
                        }

                        // -------------------------------------------------
                        // RESOLVE PATH
                        // -------------------------------------------------

                        byte[] imageBytes;
                        String storedPath = fileData.getFilePath();

                        if (storedPath.startsWith("http://")
                                        || storedPath.startsWith("https://")) {
                                imageBytes = cloudinaryService.downloadFile(
                                                storedPath,
                                                fileData.getCloudinaryPublicId(),
                                                fileData.getCloudinaryResourceType(),
                                                fileData.getFileName());
                        } else {
                                Path imagePath = Path.of(storedPath);
                                if (!imagePath.isAbsolute()) {
                                        imagePath = Path.of(System.getProperty("user.dir"))
                                                        .resolve(imagePath);
                                }
                                imageBytes = Files.readAllBytes(imagePath);
                        }

                        if (imageBytes.length == 0) {

                                throw new RuntimeException(
                                                "Image file is empty");
                        }

                        System.out.println(
                                        "IMAGE SIZE: "
                                                        + imageBytes.length
                                                        + " bytes");

                        // -------------------------------------------------
                        // VISION MODEL
                        // -------------------------------------------------

                        String answer = openRouterService.askAboutImage(
                                        question,
                                        imageBytes,
                                        mimeType);

                        long elapsed = System.currentTimeMillis()
                                        - startTime;

                        System.out.println(
                                        "IMAGE RESPONSE TIME: "
                                                        + elapsed
                                                        + " ms");

                        return Map.of(
                                        "answer",
                                        answer);

                } catch (Exception e) {

                        e.printStackTrace();

                        throw new RuntimeException(
                                        "Could not analyze image: "
                                                        + e.getMessage(),
                                        e);
                }
        }

        @PostMapping("/rename")
        public Map<String, String> renameFileWithAI(
                        @RequestBody Map<String, Object> request) {

                Object fileIdValue = request.get("fileId");

                if (fileIdValue == null) {
                        throw new RuntimeException("fileId is missing");
                }

                Long fileId = Long.parseLong(String.valueOf(fileIdValue));
                FileData fileData = fileDataService.getFileForAI(fileId);
                String content = fileTextService.extractText(fileData);
                String suggestedName = openRouterService.suggestFileName(
                                fileData.getFileName(),
                                content);

                Map<String, String> response = new HashMap<>();
                response.put("suggestedName", suggestedName);
                return response;
        }
}
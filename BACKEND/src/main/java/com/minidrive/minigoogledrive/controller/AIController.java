package com.minidrive.minigoogledrive.controller;

import com.minidrive.minigoogledrive.model.FileData;
import com.minidrive.minigoogledrive.service.ChatService;
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

@RestController
@RequestMapping("/ai")
public class AIController {

        private final OpenAIService openAIService;
        private final FileTextService fileTextService;
        private final FileDataService fileDataService;
        private final ChatService chatService;
        private final OpenRouterService openRouterService;
        private final RagService ragService;

        public AIController(
                        OpenAIService openAIService,
                        FileDataService fileDataService,
                        ChatService chatService,
                        OpenRouterService openRouterService,
                        FileTextService fileTextService,
                        RagService ragService) {

                this.openAIService = openAIService;
                this.fileDataService = fileDataService;
                this.chatService = chatService;
                this.openRouterService = openRouterService;
                this.fileTextService = fileTextService;
                this.ragService = ragService;
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

                        File file = new File(fileData.getFilePath());

                        if (!file.isAbsolute()) {

                                file = new File(
                                                System.getProperty("user.dir"),
                                                fileData.getFilePath());
                        }

                        System.out.println(
                                        "IMAGE PATH: "
                                                        + file.getAbsolutePath());

                        // -------------------------------------------------
                        // CHECK FILE
                        // -------------------------------------------------

                        if (!file.exists()) {

                                throw new RuntimeException(
                                                "Image file does not exist: "
                                                                + file.getAbsolutePath());
                        }

                        if (!file.isFile()) {

                                throw new RuntimeException(
                                                "Selected path is not a file");
                        }

                        // -------------------------------------------------
                        // READ IMAGE
                        // -------------------------------------------------

                        byte[] imageBytes = Files.readAllBytes(
                                        file.toPath());

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
}
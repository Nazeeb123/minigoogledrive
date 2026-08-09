
package com.minidrive.minigoogledrive.controller;

import com.minidrive.minigoogledrive.model.ChatMessage;
import com.minidrive.minigoogledrive.model.FileData;
import com.minidrive.minigoogledrive.service.ChatService;
import com.minidrive.minigoogledrive.service.FileDataService;
import com.minidrive.minigoogledrive.service.OpenAIService;
import com.minidrive.minigoogledrive.service.OllamaService;
import net.sourceforge.tess4j.Tesseract;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.Map;

import javax.imageio.ImageIO;
import java.nio.file.Files;

@RestController
@RequestMapping("/ai")
public class AIController {

    private final OpenAIService openAIService;
    private final FileDataService fileDataService;
    private final ChatService chatService;
    private final OllamaService ollamaService;

    public AIController(
            OpenAIService openAIService,
            FileDataService fileDataService,
            ChatService chatService,
            OllamaService ollamaService) {

        this.openAIService = openAIService;
        this.fileDataService = fileDataService;
        this.chatService = chatService;
        this.ollamaService = ollamaService;
    }
    // =========================
    // NORMAL AI
    // =========================

    @PostMapping("/ask")
    public String askAI(
            @RequestBody Map<String, String> request) {

        String question = request.get("question");

        return openAIService.askAI(question);
    }

    // =========================
    // CHAT WITH FILE
    // =========================

    @PostMapping("/file-ask")
    public Map<String, String> askAboutFile(
            @RequestBody Map<String, Object> request) {

        System.out.println("AI FILE REQUEST: " + request);

        // =========================
        // GET QUESTION
        // =========================

        Object questionObject = request.get("question");

        if (questionObject == null) {
            throw new RuntimeException("Question is missing from request");
        }

        String question = String.valueOf(questionObject).trim();

        if (question.isEmpty()) {
            throw new RuntimeException("Question cannot be empty");
        }

        // =========================
        // GET FILE ID
        // =========================

        Object fileIdObject = request.get("fileId");

        System.out.println("QUESTION: " + question);
        System.out.println("FILE ID OBJECT: " + fileIdObject);

        if (fileIdObject == null) {
            throw new RuntimeException("fileId is missing from request");
        }

        Long fileId;

        try {

            if (fileIdObject instanceof Number) {

                fileId = ((Number) fileIdObject).longValue();

            } else {

                String fileIdString = String.valueOf(fileIdObject).trim();

                if (fileIdString.isEmpty()
                        || fileIdString.equalsIgnoreCase("null")) {

                    throw new RuntimeException(
                            "fileId is null or empty");
                }

                fileId = Long.parseLong(fileIdString);
            }

        } catch (NumberFormatException e) {

            throw new RuntimeException(
                    "Invalid fileId: " + fileIdObject);
        }

        // =========================
        // GET FILE
        // =========================

        try {

            FileData fileData = fileDataService.getFileForAI(fileId);

            System.out.println(
                    "AI FILE NAME: " +
                            fileData.getFileName());

            System.out.println(
                    "AI FILE PATH: " +
                            fileData.getFilePath());

            // =========================
            // EXTRACT FILE CONTENT
            // =========================

            String content = extractText(fileData);

            if (content == null
                    || content.trim().isEmpty()) {

                return Map.of(
                        "answer",
                        "I could not extract readable text from this file.");
            }

            // Prevent sending extremely large files
            if (content.length() > 50000) {

                content = content.substring(0, 50000);
            }

            // =========================
            // ASK OLLAMA
            // =========================

            String answer = ollamaService.askAboutFile(
                    question,
                    fileData.getFileName(),
                    content);

            return Map.of(
                    "answer",
                    answer);

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Could not process file: "
                            + e.getMessage(),
                    e);
        }
    }

    // =========================
    // EXTRACT FILE TEXT
    // =========================

    private String extractText(FileData fileData) {

        String path = fileData.getFilePath();

        String fileType = fileData.getFileType();

        File file = new File(path);

        System.out.println(
                "AI FILE PATH: " + path);

        System.out.println(
                "AI FILE TYPE: " + fileType);

        System.out.println(
                "AI FILE EXISTS: " + file.exists());

        // =========================
        // PDF
        // =========================

        if ("application/pdf".equalsIgnoreCase(fileType)) {

            try (PDDocument document = Loader.loadPDF(file)) {

                PDFTextStripper stripper = new PDFTextStripper();

                return stripper.getText(document);

            } catch (IOException e) {

                throw new RuntimeException(
                        "Could not read PDF file",
                        e);
            }
        }

        // =========================
        // DOCX
        // =========================

        if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                .equalsIgnoreCase(fileType)) {

            try (
                    FileInputStream input = new FileInputStream(file);

                    XWPFDocument document = new XWPFDocument(input)) {

                StringBuilder text = new StringBuilder();

                document.getParagraphs()
                        .forEach(paragraph -> text.append(
                                paragraph.getText()).append("\n"));

                return text.toString();

            } catch (IOException e) {

                throw new RuntimeException(
                        "Could not read DOCX file",
                        e);
            }
        }

        // =========================
        // DOC
        // =========================

        if ("application/msword"
                .equalsIgnoreCase(fileType)) {

            try (
                    FileInputStream input = new FileInputStream(file);

                    HWPFDocument document = new HWPFDocument(input);

                    WordExtractor extractor = new WordExtractor(document)) {

                return extractor.getText();

            } catch (IOException e) {

                throw new RuntimeException(
                        "Could not read DOC file",
                        e);
            }
        }

        // =========================
        // TXT
        // =========================

        // =========================
        // TXT
        // =========================

        if ("text/plain".equalsIgnoreCase(fileType)) {

            try {

                System.out.println("READING TXT FILE...");
                System.out.println("TXT ABSOLUTE PATH: " + file.getAbsolutePath());
                System.out.println("TXT FILE EXISTS: " + file.exists());
                System.out.println("TXT FILE SIZE: " + file.length());

                if (!file.exists()) {
                    throw new RuntimeException(
                            "TXT file does not exist: "
                                    + file.getAbsolutePath());
                }

                if (file.length() == 0) {
                    return "";
                }

                byte[] bytes = Files.readAllBytes(file.toPath());

                System.out.println(
                        "TXT BYTES READ: " + bytes.length);

                // UTF-8 BOM
                if (bytes.length >= 3
                        && (bytes[0] & 0xFF) == 0xEF
                        && (bytes[1] & 0xFF) == 0xBB
                        && (bytes[2] & 0xFF) == 0xBF) {

                    return new String(
                            bytes,
                            3,
                            bytes.length - 3,
                            java.nio.charset.StandardCharsets.UTF_8);
                }

                // UTF-8
                String text = new String(
                        bytes,
                        java.nio.charset.StandardCharsets.UTF_8);

                // If UTF-8 decoded correctly
                if (!text.contains("\uFFFD")) {
                    return text;
                }

                // Windows encoding fallback
                return new String(
                        bytes,
                        java.nio.charset.Charset.forName(
                                "Windows-1252"));

            } catch (Exception e) {

                e.printStackTrace();

                throw new RuntimeException(
                        "Could not read TXT file: "
                                + e.getMessage(),
                        e);
            }
        }
        // =========================
        // JPG / JPEG / PNG
        // =========================

        if ("image/jpeg".equalsIgnoreCase(fileType)
                || "image/jpg".equalsIgnoreCase(fileType)
                || "image/png".equalsIgnoreCase(fileType)) {

            return extractImageText(file);
        }

        // =========================
        // UNKNOWN FILE TYPE
        // =========================

        return "";
    }

    // =========================
    // OCR IMAGE
    // =========================

    private String extractImageText(File file) {

        try {

            BufferedImage image = ImageIO.read(file);

            if (image == null) {
                throw new RuntimeException(
                        "Could not load image.");
            }

            Tesseract tesseract = new Tesseract();

            // Tesseract installation directory
            String tessdataPath = "C:/Program Files/Tesseract-OCR/tessdata";

            File tessdataFolder = new File(tessdataPath);

            if (!tessdataFolder.exists()) {
                throw new RuntimeException(
                        "Tesseract tessdata folder not found: "
                                + tessdataPath);
            }

            // Check English language data
            File englishData = new File(
                    tessdataFolder,
                    "eng.traineddata");

            if (!englishData.exists()) {
                throw new RuntimeException(
                        "Tesseract English language file not found: "
                                + englishData.getAbsolutePath());
            }

            tesseract.setDatapath(tessdataPath);
            tesseract.setLanguage("eng");

            return tesseract.doOCR(image);

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Could not read image using OCR: "
                            + e.getMessage(),
                    e);
        }
    }
    // =========================
    // AI AUTO RENAME
    // =========================

    @PostMapping("/rename")
    public Map<String, String> aiRename(
            @RequestBody Map<String, Object> request) {

        Object fileIdObject = request.get("fileId");

        if (fileIdObject == null) {
            throw new RuntimeException("fileId is missing");
        }

        Long fileId;

        try {
            fileId = Long.parseLong(
                    String.valueOf(fileIdObject));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid fileId");
        }

        FileData fileData = fileDataService.getFileForAI(fileId);

        String content = extractText(fileData);

        if (content == null || content.trim().isEmpty()) {

            throw new RuntimeException(
                    "Could not extract readable text from this file");
        }

        // Don't send an unnecessarily huge document
        if (content.length() > 15000) {
            content = content.substring(0, 15000);
        }

        String extension = "";

        String originalName = fileData.getFileName();

        int dotIndex = originalName.lastIndexOf(".");

        if (dotIndex > 0) {
            extension = originalName.substring(dotIndex);
        }

        String prompt = "You are an AI file naming assistant inside a Google Drive application.\n\n"
                + "Analyze the following file content and suggest ONE professional, "
                + "descriptive filename.\n\n"

                + "ORIGINAL FILE NAME:\n"
                + originalName
                + "\n\n"

                + "FILE CONTENT:\n"
                + content
                + "\n\n"

                + "RULES:\n"
                + "1. Give only the new filename.\n"
                + "2. Do not give explanations.\n"
                + "3. Do not use quotes.\n"
                + "4. Do not include the file extension.\n"
                + "5. Keep the name concise and professional.\n"
                + "6. Use words that describe the actual content.\n";

        String aiName = ollamaService.askAI(prompt);

        // Clean Ollama response
        aiName = aiName.trim();

        aiName = aiName
                .replace("\"", "")
                .replace("'", "")
                .replace("\n", " ")
                .replace("\r", " ")
                .trim();

        // Remove accidental extension from AI response
        int aiDotIndex = aiName.lastIndexOf(".");

        if (aiDotIndex > 0) {

            String possibleExtension = aiName.substring(aiDotIndex);

            if (possibleExtension.length() <= 10) {
                aiName = aiName.substring(
                        0,
                        aiDotIndex);
            }
        }

        String finalName = aiName + extension;

        return Map.of(
                "originalName",
                originalName,

                "suggestedName",
                finalName);
    }

}

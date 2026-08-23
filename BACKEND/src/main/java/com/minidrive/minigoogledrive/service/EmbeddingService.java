package com.minidrive.minigoogledrive.service;

import com.minidrive.minigoogledrive.model.FileData;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

    private final FileTextService fileTextService;
    private final OllamaService ollamaService;

    public EmbeddingService(
            FileTextService fileTextService,
            OllamaService ollamaService) {

        this.fileTextService = fileTextService;
        this.ollamaService = ollamaService;
    }

    // =========================================================
    // CREATE EMBEDDING FROM TEXT
    // =========================================================

    public double[] createEmbedding(String text) {

        if (text == null || text.trim().isEmpty()) {
            throw new RuntimeException(
                    "Embedding text cannot be empty");
        }

        return ollamaService.createEmbedding(text);
    }

    // =========================================================
    // CREATE EMBEDDING FROM FILE
    // =========================================================

    public double[] generateEmbedding(FileData fileData) {

        if (fileData == null) {
            throw new RuntimeException(
                    "File cannot be null");
        }

        String content = fileTextService.extractText(fileData);

        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException(
                    "Could not extract readable text from file: "
                            + fileData.getFileName());
        }

        return createEmbedding(content);
    }
}
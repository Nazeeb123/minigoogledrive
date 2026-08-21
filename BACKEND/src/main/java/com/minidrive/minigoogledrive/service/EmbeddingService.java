package com.minidrive.minigoogledrive.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minidrive.minigoogledrive.model.FileData;
import com.minidrive.minigoogledrive.repository.FileDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmbeddingService {

    private final OllamaService ollamaService;
    private final FileTextService fileTextService;
    private final FileDataRepository fileDataRepository;
    private final ObjectMapper objectMapper;

    public EmbeddingService(
            OllamaService ollamaService,
            FileTextService fileTextService,
            FileDataRepository fileDataRepository,
            ObjectMapper objectMapper) {

        this.ollamaService = ollamaService;
        this.fileTextService = fileTextService;
        this.fileDataRepository = fileDataRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void generateEmbedding(FileData fileData) {

        if (fileData == null) {
            return;
        }

        if (fileData.isDeleted()) {
            return;
        }

        if (fileData.getEmbedding() != null
                && !fileData.getEmbedding().isBlank()) {
            return;
        }

        String content = fileTextService.extractText(fileData);

        System.out.println("FILE TEXT LENGTH: " +
                (content == null ? "NULL" : content.length()));

        if (content == null
                || content.trim().isEmpty()) {

            System.out.println("NO TEXT EXTRACTED");

            return;
        }
        // Avoid sending an extremely large document
        if (content.length() > 30000) {
            content = content.substring(0, 30000);
        }

        double[] embedding = ollamaService.createEmbedding(content);
        System.out.println(
                "EMBEDDING LENGTH: " + embedding.length);

        try {

            String embeddingJson = objectMapper.writeValueAsString(embedding);

            fileData.setEmbedding(embeddingJson);

            fileDataRepository.save(fileData);

            System.out.println(
                    "EMBEDDING CREATED: "
                            + fileData.getFileName());

        } catch (JsonProcessingException e) {

            throw new RuntimeException(
                    "Could not save file embedding", e);
        }
    }
}
package com.minidrive.minigoogledrive.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minidrive.minigoogledrive.model.DocumentChunk;
import com.minidrive.minigoogledrive.model.FileData;
import com.minidrive.minigoogledrive.repository.DocumentChunkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingService {

    private final OllamaService ollamaService;
    private final FileTextService fileTextService;
    private final DocumentChunkRepository documentChunkRepository;
    private final ObjectMapper objectMapper;

    public EmbeddingService(
            OllamaService ollamaService,
            FileTextService fileTextService,
            DocumentChunkRepository documentChunkRepository,
            ObjectMapper objectMapper) {

        this.ollamaService = ollamaService;
        this.fileTextService = fileTextService;
        this.documentChunkRepository = documentChunkRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void generateEmbedding(FileData fileData) {

        if (fileData == null) {
            System.out.println("EMBEDDING SKIPPED: fileData is null");
            return;
        }

        if (fileData.isDeleted()) {
            System.out.println("EMBEDDING SKIPPED: file is deleted");
            return;
        }

        System.out.println("=================================");
        System.out.println("RAG EMBEDDING STARTED");
        System.out.println("FILE: " + fileData.getFileName());
        System.out.println("=================================");

        // -----------------------------------------
        // EXTRACT TEXT
        // -----------------------------------------

        String content;

        try {

            content = fileTextService.extractText(fileData);

        } catch (Exception e) {

            System.out.println("TEXT EXTRACTION FAILED");
            System.out.println("REASON: " + e.getMessage());

            // IMPORTANT:
            // File upload should NOT fail because
            // text extraction failed.
            return;
        }

        if (content == null || content.trim().isEmpty()) {

            System.out.println("NO TEXT EXTRACTED");
            System.out.println("SKIPPING EMBEDDING");

            return;
        }

        System.out.println(
                "TOTAL TEXT LENGTH: " + content.length());

        // -----------------------------------------
        // DELETE OLD CHUNKS
        // -----------------------------------------

        try {

            documentChunkRepository.deleteByFile(fileData);

        } catch (Exception e) {

            System.out.println("OLD CHUNKS DELETE FAILED");
            System.out.println("REASON: " + e.getMessage());

            // Do not break file upload
            return;
        }

        // -----------------------------------------
        // CREATE CHUNKS
        // -----------------------------------------

        List<String> chunks = createChunks(content);

        System.out.println(
                "TOTAL CHUNKS CREATED: " + chunks.size());

        // -----------------------------------------
        // EMBED EACH CHUNK
        // -----------------------------------------

        int chunkIndex = 0;

        for (String chunkText : chunks) {

            try {

                System.out.println(
                        "Creating embedding for chunk: "
                                + chunkIndex);

                double[] embedding = ollamaService.createEmbedding(chunkText);

                // -----------------------------------------
                // CHECK EMBEDDING
                // -----------------------------------------

                if (embedding == null || embedding.length == 0) {

                    System.out.println(
                            "EMBEDDING FAILED - EMPTY RESULT");

                    System.out.println(
                            "SKIPPING CHUNK: "
                                    + chunkIndex);

                    chunkIndex++;

                    continue;
                }

                // -----------------------------------------
                // CONVERT EMBEDDING TO JSON
                // -----------------------------------------

                String embeddingJson = objectMapper.writeValueAsString(embedding);

                // -----------------------------------------
                // CREATE DOCUMENT CHUNK
                // -----------------------------------------

                DocumentChunk chunk = new DocumentChunk();

                chunk.setFile(fileData);
                chunk.setContent(chunkText);
                chunk.setEmbedding(embeddingJson);
                chunk.setChunkIndex(chunkIndex);

                // -----------------------------------------
                // SAVE CHUNK
                // -----------------------------------------

                documentChunkRepository.save(chunk);

                System.out.println(
                        "CHUNK SAVED: "
                                + chunkIndex
                                + " | EMBEDDING DIMENSIONS: "
                                + embedding.length);

                chunkIndex++;

            } catch (Exception e) {

                // =========================================
                // IMPORTANT FIX
                // =========================================

                System.out.println(
                        "=================================");

                System.out.println(
                        "EMBEDDING FAILED FOR CHUNK "
                                + chunkIndex);

                System.out.println(
                        "REASON: "
                                + e.getMessage());

                System.out.println(
                        "Skipping embedding.");

                System.out.println(
                        "FILE UPLOAD WILL CONTINUE.");

                System.out.println(
                        "=================================");

                chunkIndex++;
            }
        }

        // -----------------------------------------
        // COMPLETED
        // -----------------------------------------

        System.out.println("=================================");
        System.out.println("RAG EMBEDDING PROCESS FINISHED");
        System.out.println("FILE: " + fileData.getFileName());
        System.out.println("CHUNKS: " + chunks.size());
        System.out.println("=================================");
    }

    // =========================================================
    // TEXT CHUNKING
    // =========================================================

    private List<String> createChunks(String text) {

        List<String> chunks = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return chunks;
        }

        text = text.trim();

        // Around 1500 characters per chunk
        int chunkSize = 1500;

        // Small overlap between chunks
        int overlap = 200;

        int start = 0;

        while (start < text.length()) {

            int end = Math.min(
                    start + chunkSize,
                    text.length());

            String chunk = text.substring(start, end).trim();

            if (!chunk.isEmpty()) {

                chunks.add(chunk);
            }

            if (end >= text.length()) {
                break;
            }

            start = end - overlap;
        }

        return chunks;
    }
}
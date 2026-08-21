package com.minidrive.minigoogledrive.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minidrive.minigoogledrive.model.DocumentChunk;
import com.minidrive.minigoogledrive.model.FileData;
import com.minidrive.minigoogledrive.repository.DocumentChunkRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RagService {

    private final DocumentChunkRepository chunkRepository;
    private final OllamaService ollamaService;
    private final FileTextService fileTextService;
    private final ObjectMapper objectMapper;
    private final OpenRouterService openRouterService;

    public RagService(
            DocumentChunkRepository chunkRepository,
            OllamaService ollamaService,
            FileTextService fileTextService,
            ObjectMapper objectMapper,
            OpenRouterService openRouterService) {

        this.chunkRepository = chunkRepository;
        this.ollamaService = ollamaService;
        this.fileTextService = fileTextService;
        this.objectMapper = objectMapper;
        this.openRouterService = openRouterService;
    }

    // =========================================================
    // CREATE CHUNKS + EMBEDDINGS
    // =========================================================

    @Transactional
    public void createChunks(FileData fileData) {

        if (fileData == null) {
            return;
        }

        System.out.println(
                "RAG: Creating chunks for "
                        + fileData.getFileName());

        String content = fileTextService.extractText(fileData);

        if (content == null ||
                content.trim().isEmpty()) {

            System.out.println(
                    "RAG: No readable text found");

            return;
        }

        // -----------------------------------------------------
        // NORMALIZE
        // -----------------------------------------------------

        content = content
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();

        // -----------------------------------------------------
        // DELETE OLD CHUNKS
        // -----------------------------------------------------

        chunkRepository.deleteByFile(fileData);

        // -----------------------------------------------------
        // SPLIT
        // -----------------------------------------------------

        List<String> chunks = splitText(content);

        System.out.println(
                "RAG: "
                        + chunks.size()
                        + " chunks created");

        // -----------------------------------------------------
        // CREATE EMBEDDINGS
        // -----------------------------------------------------

        int index = 0;

        for (String chunkText : chunks) {

            if (chunkText == null ||
                    chunkText.trim().isEmpty()) {

                continue;
            }

            try {

                double[] embedding = ollamaService.createEmbedding(
                        chunkText);

                String embeddingJson = objectMapper.writeValueAsString(
                        embedding);

                DocumentChunk chunk = new DocumentChunk();

                chunk.setFile(fileData);
                chunk.setContent(chunkText);
                chunk.setEmbedding(embeddingJson);
                chunk.setChunkIndex(index);

                chunkRepository.save(chunk);

                System.out.println(
                        "RAG CHUNK "
                                + index
                                + " CREATED");

                index++;

            } catch (Exception e) {

                throw new RuntimeException(
                        "Could not create embedding for chunk "
                                + index,
                        e);
            }
        }

        System.out.println(
                "RAG: Chunk generation completed");
    }

    // =========================================================
    // ASK QUESTION
    // =========================================================

    public String askAboutFile(
            FileData fileData,
            String question) {

        long startTime = System.currentTimeMillis();

        if (fileData == null) {

            return "File not found.";
        }

        if (question == null ||
                question.trim().isEmpty()) {

            return "Please enter a question.";
        }

        System.out.println(
                "=================================");

        System.out.println(
                "RAG QUESTION: "
                        + question);

        System.out.println(
                "RAG FILE: "
                        + fileData.getFileName());

        System.out.println(
                "=================================");

        // =====================================================
        // LOAD EXISTING CHUNKS
        // =====================================================

        List<DocumentChunk> chunks = chunkRepository.findByFile(fileData);

        // =====================================================
        // CREATE CHUNKS ONLY IF NECESSARY
        // =====================================================

        if (chunks.isEmpty()) {

            System.out.println(
                    "RAG: No chunks found.");

            System.out.println(
                    "RAG: Creating chunks for first question...");

            createChunks(fileData);

            chunks = chunkRepository.findByFile(fileData);
        }

        if (chunks.isEmpty()) {

            return "I could not find readable information in this file.";
        }

        System.out.println(
                "RAG: Loaded "
                        + chunks.size()
                        + " existing chunks.");

        // =====================================================
        // EMBED QUESTION
        // =====================================================

        long embeddingStart = System.currentTimeMillis();

        double[] questionEmbedding = ollamaService.createEmbedding(
                question);

        long embeddingTime = System.currentTimeMillis()
                - embeddingStart;

        System.out.println(
                "QUESTION EMBEDDING TIME: "
                        + embeddingTime
                        + " ms");

        // =====================================================
        // CALCULATE SIMILARITY
        // =====================================================

        List<ScoredChunk> scoredChunks = new ArrayList<>();

        for (DocumentChunk chunk : chunks) {

            if (chunk.getEmbedding() == null ||
                    chunk.getEmbedding().isBlank()) {

                continue;
            }

            try {

                List<Double> values = objectMapper.readValue(
                        chunk.getEmbedding(),
                        new TypeReference<List<Double>>() {
                        });

                double[] chunkEmbedding = new double[values.size()];

                for (int i = 0; i < values.size(); i++) {

                    chunkEmbedding[i] = values.get(i);
                }

                double score = cosineSimilarity(
                        questionEmbedding,
                        chunkEmbedding);

                scoredChunks.add(
                        new ScoredChunk(
                                chunk,
                                score));

            } catch (Exception e) {

                System.out.println(
                        "RAG: Could not read embedding for chunk "
                                + chunk.getChunkIndex());
            }
        }

        // =====================================================
        // NO VALID EMBEDDINGS
        // =====================================================

        if (scoredChunks.isEmpty()) {

            return "I could not find enough relevant information in this file.";
        }

        // =====================================================
        // SORT
        // =====================================================

        scoredChunks.sort(
                Comparator.comparingDouble(
                        ScoredChunk::score)
                        .reversed());

        // =====================================================
        // SELECT TOP CHUNKS
        // =====================================================

        /*
         * Previously we sent 3 chunks.
         *
         * Keep the context small so OpenRouter can answer faster.
         */

        int limit = Math.min(
                2,
                scoredChunks.size());

        StringBuilder context = new StringBuilder();

        for (int i = 0; i < limit; i++) {

            DocumentChunk chunk = scoredChunks
                    .get(i)
                    .chunk();

            double score = scoredChunks
                    .get(i)
                    .score();

            System.out.println(
                    "SELECTED CHUNK: "
                            + chunk.getChunkIndex()
                            + " | SCORE: "
                            + score);

            context.append(
                    "\n--- RELEVANT SECTION ---\n");

            context.append(
                    chunk.getContent());

            context.append("\n");
        }

        // =====================================================
        // LIMIT CONTEXT SIZE
        // =====================================================

        String finalContext = context.toString();

        /*
         * Prevent unnecessarily large prompts.
         */

        int maxContextLength = 7000;

        if (finalContext.length() > maxContextLength) {

            finalContext = finalContext.substring(
                    0,
                    maxContextLength);
        }

        System.out.println(
                "RAG CONTEXT LENGTH: "
                        + finalContext.length());

        // =====================================================
        // SEND TO OPENROUTER
        // =====================================================

        long aiStart = System.currentTimeMillis();

        String answer = openRouterService.askAboutFile(
                question,
                fileData.getFileName(),
                finalContext);

        long aiTime = System.currentTimeMillis()
                - aiStart;

        long totalTime = System.currentTimeMillis()
                - startTime;

        System.out.println(
                "OPENROUTER RESPONSE TIME: "
                        + aiTime
                        + " ms");

        System.out.println(
                "TOTAL RAG RESPONSE TIME: "
                        + totalTime
                        + " ms");

        return answer;
    }

    // =========================================================
    // TEXT CHUNKING
    // =========================================================

    private List<String> splitText(
            String text) {

        List<String> chunks = new ArrayList<>();

        /*
         * Smaller chunks = smaller embeddings
         * and more focused retrieval.
         */

        int chunkSize = 1000;

        /*
         * Small overlap preserves context
         * between neighboring chunks.
         */

        int overlap = 100;

        int start = 0;

        while (start < text.length()) {

            int end = Math.min(
                    start + chunkSize,
                    text.length());

            String chunk = text.substring(
                    start,
                    end)
                    .trim();

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

    // =========================================================
    // COSINE SIMILARITY
    // =========================================================

    private double cosineSimilarity(
            double[] a,
            double[] b) {

        if (a == null ||
                b == null) {

            return 0;
        }

        if (a.length != b.length) {

            throw new RuntimeException(
                    "Embedding dimensions do not match: "
                            + a.length
                            + " vs "
                            + b.length);
        }

        double dot = 0;

        double magnitudeA = 0;

        double magnitudeB = 0;

        for (int i = 0; i < a.length; i++) {

            dot += a[i] * b[i];

            magnitudeA += a[i] * a[i];

            magnitudeB += b[i] * b[i];
        }

        if (magnitudeA == 0 ||
                magnitudeB == 0) {

            return 0;
        }

        return dot /
                (Math.sqrt(magnitudeA)
                        *
                        Math.sqrt(magnitudeB));
    }

    // =========================================================
    // INTERNAL CLASS
    // =========================================================

    private record ScoredChunk(
            DocumentChunk chunk,
            double score) {
    }
}
package com.minidrive.minigoogledrive.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minidrive.minigoogledrive.model.FileData;
import com.minidrive.minigoogledrive.model.User;
import com.minidrive.minigoogledrive.repository.FileDataRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SemanticSearchService {

        private final OllamaService ollamaService;
        private final FileDataRepository fileDataRepository;
        private final ObjectMapper objectMapper;

        public SemanticSearchService(
                        OllamaService ollamaService,
                        FileDataRepository fileDataRepository,
                        ObjectMapper objectMapper) {

                this.ollamaService = ollamaService;
                this.fileDataRepository = fileDataRepository;
                this.objectMapper = objectMapper;
        }

        public List<FileData> search(
                        String query,
                        User user) {

                double[] queryEmbedding = ollamaService.createEmbedding(query);

                List<FileData> files = fileDataRepository.findByUserAndDeletedFalse(user);

                List<SearchResult> results = new ArrayList<>();

                for (FileData file : files) {

                        if (file.getEmbedding() == null
                                        || file.getEmbedding().isBlank()) {
                                continue;
                        }

                        try {

                                List<Double> storedEmbedding = objectMapper.readValue(
                                                file.getEmbedding(),
                                                new TypeReference<List<Double>>() {
                                                });

                                double similarity = cosineSimilarity(
                                                queryEmbedding,
                                                storedEmbedding);

                                results.add(
                                                new SearchResult(
                                                                file,
                                                                similarity));

                        } catch (Exception e) {

                                System.out.println(
                                                "Could not read embedding for: "
                                                                + file.getFileName());
                        }
                }

                results.sort(
                                Comparator.comparingDouble(
                                                SearchResult::score).reversed());

                return results.stream()
                                .filter(result -> result.score() > 0.50)
                                .map(SearchResult::file)
                                .limit(20)
                                .toList();
        }

        private double cosineSimilarity(
                        double[] a,
                        List<Double> b) {

                if (a.length != b.size()) {
                        return 0.0;
                }

                double dotProduct = 0.0;
                double magnitudeA = 0.0;
                double magnitudeB = 0.0;

                for (int i = 0; i < a.length; i++) {

                        dotProduct += a[i] * b.get(i);

                        magnitudeA += a[i] * a[i];

                        magnitudeB += b.get(i) * b.get(i);
                }

                if (magnitudeA == 0
                                || magnitudeB == 0) {

                        return 0.0;
                }

                return dotProduct /
                                (Math.sqrt(magnitudeA)
                                                * Math.sqrt(magnitudeB));
        }

        private record SearchResult(
                        FileData file,
                        double score) {
        }
}

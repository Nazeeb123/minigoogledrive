
package com.minidrive.minigoogledrive.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class OllamaService {

        private final RestTemplate restTemplate = new RestTemplate();

        private static final String OLLAMA_URL = "http://localhost:11434/api/generate";

        public String askAI(String question) {

                Map<String, Object> request = Map.of(
                                "model", "qwen2.5:3b",
                                "prompt", question,
                                "stream", false);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

                ResponseEntity<Map> response = restTemplate.postForEntity(
                                OLLAMA_URL,
                                entity,
                                Map.class);

                if (response.getBody() == null) {
                        throw new RuntimeException("No response from Ollama");
                }

                Object answer = response.getBody().get("response");

                if (answer == null) {
                        throw new RuntimeException("Ollama returned no answer");
                }

                return answer.toString();
        }

        public String askAboutFile(
                        String question,
                        String fileName,
                        String content) {

                String prompt = "You are an AI assistant inside a Google Drive application.\n\n"
                                + "The user is asking a question about the file below.\n\n"
                                + "FILE NAME:\n"
                                + fileName
                                + "\n\n"
                                + "FILE CONTENT:\n"
                                + content
                                + "\n\n"
                                + "USER QUESTION:\n"
                                + question
                                + "\n\n"
                                + "Answer the user's question using the file content. "
                                + "If the answer cannot be found in the file, clearly say so. "
                                + "Keep the answer clear and useful.";

                return askAI(prompt);
        }

        public double[] createEmbedding(String text) {

                if (text == null || text.trim().isEmpty()) {
                        throw new RuntimeException("Embedding text cannot be empty");
                }

                Map<String, Object> request = Map.of(
                                "model", "nomic-embed-text",
                                "input", text);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

                try {

                        ResponseEntity<Map> response = restTemplate.postForEntity(
                                        "http://localhost:11434/api/embed",
                                        entity,
                                        Map.class);

                        if (response.getBody() == null) {
                                throw new RuntimeException(
                                                "No response from Ollama embedding API");
                        }

                        Object embeddingsObject = response.getBody().get("embeddings");

                        if (embeddingsObject == null) {
                                throw new RuntimeException(
                                                "Ollama returned no embeddings: "
                                                                + response.getBody());
                        }

                        /*
                         * /api/embed returns:
                         *
                         * {
                         * "embeddings": [
                         * [0.123, -0.456, ...]
                         * ]
                         * }
                         */

                        java.util.List<?> embeddings = (java.util.List<?>) embeddingsObject;

                        if (embeddings.isEmpty()) {
                                throw new RuntimeException(
                                                "Ollama returned an empty embedding");
                        }

                        java.util.List<?> vector = (java.util.List<?>) embeddings.get(0);

                        double[] result = new double[vector.size()];

                        for (int i = 0; i < vector.size(); i++) {

                                result[i] = ((Number) vector.get(i)).doubleValue();
                        }

                        System.out.println(
                                        "OLLAMA EMBEDDING CREATED. DIMENSIONS: "
                                                        + result.length);

                        return result;

                } catch (Exception e) {

                        System.out.println(
                                        "OLLAMA EMBEDDING ERROR: "
                                                        + e.getMessage());

                        throw new RuntimeException(
                                        "Failed to create embedding using Ollama",
                                        e);
                }
        }
}

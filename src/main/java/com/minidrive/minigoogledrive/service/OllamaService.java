
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
}

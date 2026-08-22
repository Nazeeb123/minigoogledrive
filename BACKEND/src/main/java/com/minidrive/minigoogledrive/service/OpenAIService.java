package com.minidrive.minigoogledrive.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class OpenAIService {

    private final RestTemplate restTemplate;

    public OpenAIService() {
        this.restTemplate = new RestTemplate();
    }

    // Normal AI question
    public String askAI(String question) {

        String url = "http://localhost:11434/api/generate";

        Map<String, Object> request = Map.of(
                "model", "qwen2.5:3b",
                "prompt", question,
                "stream", false
        );

        Map<String, Object> response =
                restTemplate.postForObject(
                        url,
                        request,
                        Map.class
                );

        if (response == null) {
            return "No response from Ollama";
        }

        Object answer = response.get("response");

        if (answer == null) {
            return "No response from Ollama";
        }

        return answer.toString();
    }


    // AI question using file content
    public String askAboutFile(
            String question,
            String fileName,
            String fileContent) {

        String url =
                "http://localhost:11434/api/generate";

        String prompt = """
                You are an AI assistant inside Mini Google Drive.

                The user has selected the following file:

                FILE NAME:
                %s

                FILE CONTENT:
                -------------------------
                %s
                -------------------------

                USER QUESTION:
                %s

                Instructions:
                - Answer using the file content whenever possible.
                - If the answer is not present in the file, clearly say that.
                - Do not invent information from the document.
                - Explain things simply when appropriate.
                """.formatted(
                fileName,
                fileContent,
                question
        );

        Map<String, Object> request = Map.of(
                "model", "qwen2.5:3b",
                "prompt", prompt,
                "stream", false
        );

        Map<String, Object> response =
                restTemplate.postForObject(
                        url,
                        request,
                        Map.class
                );

        if (response == null) {
            return "No response from Ollama";
        }

        Object answer = response.get("response");

        if (answer == null) {
            return "No response from Ollama";
        }

        return answer.toString();
    }
}
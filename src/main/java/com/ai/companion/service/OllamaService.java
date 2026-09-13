package com.ai.companion.service;

import com.ai.companion.dto.OllamaRequest;
import com.ai.companion.dto.OllamaResponse;
import com.ai.companion.exception.OllamaException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class OllamaService {

    private final RestClient client;

    public OllamaService() {
        client = RestClient.builder()
                .baseUrl("http://localhost:11434")
                .build();
    }

    public String generate(String prompt) {

        OllamaRequest request = new OllamaRequest(
                "qwen3:4b",
                prompt,
                false
        );

        try {
            OllamaResponse response = client.post()
                    .uri("/api/generate")
                    .body(request)
                    .retrieve()
                    .body(OllamaResponse.class);

            if (response == null) {
                throw new OllamaException(
                        "Ollama returned an empty response.",
                        null
                );
            }

            return response.response();

        } catch (RestClientException ex) {
            throw new OllamaException(
                    "Unable to communicate with Ollama.",
                    ex
            );
        }
    }
}
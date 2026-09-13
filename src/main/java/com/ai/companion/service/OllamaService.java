package com.ai.companion.service;

import com.ai.companion.dto.ChatMessage;
import com.ai.companion.dto.OllamaChatRequest;
import com.ai.companion.dto.OllamaChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class OllamaService {

    private final RestClient client;

    public OllamaService() {
        client = RestClient.builder()
                .baseUrl("http://localhost:11434")
                .build();
    }

    public String generate(List<ChatMessage> messages) {

        OllamaChatRequest request = new OllamaChatRequest(
                "qwen3:4b",
                messages,
                false
        );

        OllamaChatResponse response = client.post()
                .uri("/api/chat")
                .body(request)
                .retrieve()
                .body(OllamaChatResponse.class);

        assert response != null;

        return response.message().content();
    }
}
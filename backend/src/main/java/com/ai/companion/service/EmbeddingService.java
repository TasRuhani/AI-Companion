package com.ai.companion.service;

import com.ai.companion.dto.OllamaEmbeddingRequest;
import com.ai.companion.dto.OllamaEmbeddingResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class EmbeddingService {

    private final RestClient restClient;

    public EmbeddingService() {
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:11434")
                .build();
    }

    public List<Float> embed(String text) {

        OllamaEmbeddingRequest request =
                new OllamaEmbeddingRequest(
                        "bge-m3",
                        text
                );

        OllamaEmbeddingResponse response =
                restClient.post()
                        .uri("/api/embed")
                        .body(request)
                        .retrieve()
                        .body(OllamaEmbeddingResponse.class);

        return response.embeddings().get(0);
    }
}
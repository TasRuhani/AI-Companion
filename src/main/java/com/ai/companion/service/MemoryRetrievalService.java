package com.ai.companion.service;

import com.ai.companion.entity.Memory;
import com.ai.companion.repository.MemoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemoryRetrievalService {

    private final EmbeddingService embeddingService;
    private final MemoryRepository memoryRepository;

    public MemoryRetrievalService(
            EmbeddingService embeddingService,
            MemoryRepository memoryRepository) {
        this.embeddingService = embeddingService;
        this.memoryRepository = memoryRepository;
    }

    public List<Memory> retrieveRelevantMemories(
            String query,
            int limit) {

        List<Float> embedding =
                embeddingService.embed(query);

        if (embedding.size() != 1024) {
            throw new IllegalStateException(
                    "Expected 1024-dimensional embedding, got "
                            + embedding.size()
            );
        }

        String vector = toPgVector(embedding);

        return memoryRepository.findSimilarActiveMemories(
                vector,
                0.45,
                limit
        );
    }

    private String toPgVector(List<Float> embedding) {

        StringBuilder vector = new StringBuilder("[");

        for (int i = 0; i < embedding.size(); i++) {
            if (i > 0) {
                vector.append(",");
            }

            vector.append(embedding.get(i));
        }

        vector.append("]");

        return vector.toString();
    }
}
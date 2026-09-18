package com.ai.companion.service;

import com.ai.companion.dto.MemoryRequest;
import com.ai.companion.dto.MemoryResponse;
import com.ai.companion.entity.Memory;
import com.ai.companion.repository.MemoryRepository;
import com.ai.companion.service.EmbeddingService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MemoryService {

    private final MemoryRepository memoryRepository;

    private final EmbeddingService embeddingService;

    public MemoryService(MemoryRepository memoryRepository, EmbeddingService embeddingService) {
        this.memoryRepository = memoryRepository;
        this.embeddingService = embeddingService;
    }

    public List<Memory> getActiveMemories() {
        return memoryRepository.findByActiveTrue();
    }

    public MemoryResponse save(MemoryRequest request) {

        Memory memory = new Memory();

        memory.setContent(request.content());
        memory.setRawExcerpt(request.rawExcerpt());
        memory.setCategory(request.category());
        memory.setImportance(request.importance());
        memory.setConfidence(request.confidence());
        memory.setSource(request.source());

        memory.setReferenceCount(0);
        memory.setActive(true);

        LocalDateTime now = LocalDateTime.now();
        memory.setCreatedAt(now);
        memory.setUpdatedAt(now);

        Memory saved = memoryRepository.save(memory);

        return toResponse(saved);
    }

    public Memory save(Memory memory) {

        if (memory.getSupersedesId() != null) {

            Optional<Memory> oldMemory =
                    memoryRepository.findById(memory.getSupersedesId());

            oldMemory.ifPresent(old -> {
                if (old.isActive()) {
                    old.setActive(false);
                    memoryRepository.save(old);
                }
            });
        }

        List<Float> embedding = embeddingService.embed(memory.getContent());

        float[] vector = new float[embedding.size()];

        if (embedding.size() != 1024) {
            throw new IllegalStateException(
                    "Expected 1024 D embedding bit got " + embedding.size()
            );
        }

        for (int i = 0; i < embedding.size(); i++) {
            vector[i] = embedding.get(i);
        }

        memory.setEmbedding(vector);

        return memoryRepository.save(memory);
    }

    public List<MemoryResponse> getAll() {
        return memoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private MemoryResponse toResponse(Memory memory) {

        return new MemoryResponse(
                memory.getId(),
                memory.getContent(),
                memory.getRawExcerpt(),
                memory.getCategory(),
                memory.getImportance(),
                memory.getConfidence(),
                memory.getCreatedAt(),
                memory.getUpdatedAt(),
                memory.getLastReferencedAt(),
                memory.getReferenceCount(),
                memory.getSupersedesId(),
                memory.isActive(),
                memory.getSource()
        );
    }

    public void recordReference (Memory memory) {
        memory.setReferenceCount( memory.getReferenceCount() + 1);

        memory.setLastReferencedAt(LocalDateTime.now());

        memoryRepository.save(memory);
    }
}
package com.ai.companion.dto;

import com.ai.companion.entity.MemoryCategory;
import com.ai.companion.entity.MemorySource;

import java.time.LocalDateTime;

public record MemoryResponse(
        Long id,
        String content,
        String rawExcerpt,
        MemoryCategory category,
        int importance,
        double confidence,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastReferencedAt,
        int referenceCount,
        Long supersedesId,
        boolean active,
        MemorySource source
) {
}
package com.ai.companion.dto;

import com.ai.companion.entity.MemoryCategory;
import com.fasterxml.jackson.annotation.JsonProperty;

public record MemoryEvaluation(
        boolean worthRemembering,
        String content,
        @JsonProperty("category")
        MemoryCategory memoryCategory,
        int importance,
        double confidence,
        Long supersedesId
) {

    public static MemoryEvaluation notWorthRemembering() {
        return new MemoryEvaluation(
                false,
                null,
                null,
                0,
                1.0,
                null
        );
    }
}
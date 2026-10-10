package com.ai.companion.dto;

import com.ai.companion.entity.MemoryCategory;
import com.ai.companion.entity.MemorySource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MemoryRequest(

        @NotBlank
        String content,

        String rawExcerpt,

        @NotNull
        MemoryCategory category,

        @Min(1)
        @Max(10)
        int importance,

        @Min(0)
        @Max(1)
        double confidence,

        @NotNull
        MemorySource source
) {
}
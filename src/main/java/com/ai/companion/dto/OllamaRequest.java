package com.ai.companion.dto;

import jakarta.validation.constraints.NotBlank;

public record OllamaRequest(
        String model,
        @NotBlank String prompt,
        boolean stream
) {
}
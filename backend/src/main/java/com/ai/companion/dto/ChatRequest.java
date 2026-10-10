package com.ai.companion.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank String message) {}
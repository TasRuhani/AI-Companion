package com.ai.companion.dto;

public record OllamaChatResponse(
        String model,
        ChatMessage message,
        boolean done
) {
}
package com.ai.companion.dto;

import java.util.List;

public record OllamaChatRequest(
        String model,
        List<ChatMessage> messages,
        boolean stream
) {
}
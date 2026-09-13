package com.ai.companion.service;

import org.springframework.stereotype.Service;

@Service
public class ChatService {
    private final OllamaService ollamaService;

    public ChatService(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    public String chat(String message) {
        return ollamaService.generate(message);
    }
}

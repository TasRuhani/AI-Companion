package com.ai.companion.controller;

import com.ai.companion.dto.ChatRequest;
import com.ai.companion.dto.ChatResponse;
import com.ai.companion.service.ChatService;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat (@Valid @RequestBody ChatRequest request) {
        return new ChatResponse(chatService.chat(request.message()));
    }
}

package com.ai.companion.service;

import com.ai.companion.dto.ChatMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    private final OllamaService ollamaService;
    private final RedisTemplate<String, ChatMessage> redisTemplate;

    public ChatService(
            OllamaService ollamaService,
            RedisTemplate<String, ChatMessage> redisTemplate
    ) {
        this.ollamaService = ollamaService;
        this.redisTemplate = redisTemplate;
    }

    public String chat(String message) {

        ChatMessage userMessage = new ChatMessage(
                "user",
                message
        );

        redisTemplate.opsForList()
                .rightPush("conversation", userMessage);

        List<ChatMessage> messages = getConversation();

        String response = ollamaService.generate(messages);

        ChatMessage assistantMessage = new ChatMessage(
                "assistant",
                response
        );

        redisTemplate.opsForList()
                .rightPush("conversation", assistantMessage);

        return response;
    }

    private List<ChatMessage> getConversation() {
        return redisTemplate.opsForList()
                .range("conversation", -20, -1);
    }
}
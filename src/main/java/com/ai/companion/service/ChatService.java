package com.ai.companion.service;

import com.ai.companion.dto.ChatMessage;
import com.ai.companion.dto.MemoryEvaluation;
import com.ai.companion.entity.Memory;
import com.ai.companion.entity.MemorySource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {

    private final OllamaService ollamaService;
    private final RedisTemplate<String, ChatMessage> redisTemplate;
    private final MemoryService memoryService;
    private final MemoryRetrievalService memoryRetrievalService;

    public ChatService(
            OllamaService ollamaService,
            RedisTemplate<String, ChatMessage> redisTemplate,
            MemoryService memoryService,
            MemoryRetrievalService memoryRetrievalService) {

        this.ollamaService = ollamaService;
        this.redisTemplate = redisTemplate;
        this.memoryService = memoryService;
        this.memoryRetrievalService = memoryRetrievalService;
    }

    public String chat(String message) {

        ChatMessage userMessage = new ChatMessage(
                "user",
                message
        );

        redisTemplate.opsForList()
                .rightPush("conversation", userMessage);

        redisTemplate.opsForList()
                .trim("conversation", -200, -1);

        List<ChatMessage> messages = getConversation();

        // RAG SIIIIIIIIIIIIIIIIIIIIUUUUUUUUUUUUUUUUUUUUUUUU
        List<Memory> relevantMemories =
                memoryRetrievalService.retrieveRelevantMemories(
                        message,
                        5
                );

        // All active memories are still needed by the memory evaluator
        List<Memory> activeMemories =
                memoryService.getActiveMemories();

        System.out.println("RELEVANT MEMORIES SENT TO MODEL:");

        relevantMemories.forEach(memory ->
                System.out.println(
                        "id=" + memory.getId()
                                + " | active=" + memory.isActive()
                                + " | category=" + memory.getCategory()
                                + " | content=" + memory.getContent()
                )
        );

        String response =
                ollamaService.generate(
                        messages,
                        relevantMemories
                );

        MemoryEvaluation evaluation =
                ollamaService.evaluateMemory(
                        message,
                        activeMemories,
                        messages
                );

        if (evaluation.worthRemembering()) {

            Memory memory = new Memory();

            memory.setContent(evaluation.content());
            memory.setCategory(evaluation.memoryCategory());
            memory.setImportance(evaluation.importance());
            memory.setConfidence(evaluation.confidence());
            memory.setSupersedesId(evaluation.supersedesId());
            memory.setRawExcerpt(message);
            memory.setReferenceCount(0);
            memory.setActive(true);
            memory.setSource(MemorySource.EXTRACTED);

            LocalDateTime now = LocalDateTime.now();

            memory.setCreatedAt(now);
            memory.setUpdatedAt(now);

            System.out.println(
                    "SAVING MEMORY: content=" + memory.getContent()
                            + " | supersedesId=" + memory.getSupersedesId()
            );

            memoryService.save(memory);
        }

        System.out.println("MEMORY EVALUATION:");
        System.out.println(evaluation);

        ChatMessage assistantMessage = new ChatMessage(
                "assistant",
                response
        );

        redisTemplate.opsForList()
                .rightPush("conversation", assistantMessage);
        redisTemplate.opsForList()
                .trim("conversation", -200, -1);

        return response;
    }

    private List<ChatMessage> getConversation() {
        return redisTemplate.opsForList()
                .range("conversation", -20, -1);
    }
}
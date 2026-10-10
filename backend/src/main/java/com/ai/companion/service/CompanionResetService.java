package com.ai.companion.service;

import com.ai.companion.dto.ChatMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
public class CompanionResetService {

    private static final String CONVERSATION_KEY = "conversation";

    private final RedisTemplate<String, ChatMessage> redisTemplate;
    private final CompanionStateResetTransaction stateResetTransaction;
    private final ProactiveMessageService proactiveMessageService;
    private final CompanionPortraitService portraitService;

    @Autowired
    public CompanionResetService(
            RedisTemplate<String, ChatMessage> redisTemplate,
            CompanionStateResetTransaction stateResetTransaction,
            ProactiveMessageService proactiveMessageService,
            CompanionPortraitService portraitService) {
        this.redisTemplate = redisTemplate;
        this.stateResetTransaction = stateResetTransaction;
        this.proactiveMessageService = proactiveMessageService;
        this.portraitService = portraitService;
    }

    public CompanionResetService(
            RedisTemplate<String, ChatMessage> redisTemplate,
            CompanionStateResetTransaction stateResetTransaction,
            ProactiveMessageService proactiveMessageService) {
        this(redisTemplate, stateResetTransaction, proactiveMessageService, null);
    }

    public synchronized void reset() {
        redisTemplate.delete(CONVERSATION_KEY);
        proactiveMessageService.resetTrackingAfter(
                stateResetTransaction::clearPersistentState);
    }

    public synchronized void resetCompanion() {
        redisTemplate.delete(CONVERSATION_KEY);
        proactiveMessageService.resetTrackingAfter(
                stateResetTransaction::clearCompanionState);
    }

    public synchronized void resetUserProfile() {
        redisTemplate.delete(CONVERSATION_KEY);
        stateResetTransaction.clearUserProfile();
    }

    public synchronized void resetAll() {
        redisTemplate.delete(CONVERSATION_KEY);
        proactiveMessageService.resetTracking();
        var portraitIds = stateResetTransaction.clearEverything();
        portraitService.removeUnreferenced(portraitIds);
    }

    public synchronized <T> T withStableState(Supplier<T> operation) {
        return operation.get();
    }
}

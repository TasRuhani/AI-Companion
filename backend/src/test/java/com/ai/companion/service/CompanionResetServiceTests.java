package com.ai.companion.service;

import com.ai.companion.dto.ChatMessage;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import static org.mockito.Mockito.*;

class CompanionResetServiceTests {

    @Test
    void resetClearsConversationTrackingAndPersistentCompanionState() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, ChatMessage> redisTemplate = mock(RedisTemplate.class);
        CompanionStateResetTransaction stateReset = mock(CompanionStateResetTransaction.class);
        ProactiveMessageService proactiveMessages = mock(ProactiveMessageService.class);
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(proactiveMessages).resetTrackingAfter(any(Runnable.class));
        CompanionResetService resetService = new CompanionResetService(
                redisTemplate, stateReset, proactiveMessages);

        resetService.reset();

        verify(redisTemplate).delete("conversation");
        verify(proactiveMessages).resetTrackingAfter(any(Runnable.class));
        verify(stateReset).clearPersistentState();
    }

    @Test
    void redisFailureDoesNotReportResetSuccessOrClearDatabaseState() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, ChatMessage> redisTemplate = mock(RedisTemplate.class);
        when(redisTemplate.delete("conversation"))
                .thenThrow(new IllegalStateException("Redis unavailable"));
        CompanionStateResetTransaction stateReset = mock(CompanionStateResetTransaction.class);
        ProactiveMessageService proactiveMessages = mock(ProactiveMessageService.class);
        CompanionResetService resetService = new CompanionResetService(
                redisTemplate, stateReset, proactiveMessages);

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class, resetService::reset);

        verifyNoInteractions(stateReset, proactiveMessages);
    }
}

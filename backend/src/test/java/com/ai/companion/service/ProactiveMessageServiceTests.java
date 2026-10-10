package com.ai.companion.service;

import com.ai.companion.dto.ChatMessage;
import com.ai.companion.entity.CompanionIdentity;
import com.ai.companion.entity.CompanionMood;
import com.ai.companion.entity.ConversationMessage;
import com.ai.companion.repository.ConversationMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProactiveMessageServiceTests {

    @Test
    @SuppressWarnings("unchecked")
    void generatedProactiveMessageIsPersistedAndAddedToConversationCache() {
        OllamaService ollamaService = mock(OllamaService.class);
        MoodService moodService = mock(MoodService.class);
        RedisTemplate<String, ChatMessage> redisTemplate = mock(RedisTemplate.class);
        ListOperations<String, ChatMessage> listOperations = mock(ListOperations.class);
        CompanionIdentityService identityService = mock(CompanionIdentityService.class);
        ConversationMessageRepository repository = mock(ConversationMessageRepository.class);
        CompanionMood mood = new CompanionMood();
        mood.setMoodScore(50);
        CompanionIdentity identity = new CompanionIdentity();
        identity.setId(7L);

        when(identityService.exists()).thenReturn(true);
        when(identityService.getIdentity()).thenReturn(identity);
        when(moodService.getMood()).thenReturn(mood);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range("conversation", -20, -1)).thenReturn(List.of());
        when(ollamaService.evaluateProactiveMessage(anyLong(), eq(50), any()))
                .thenReturn("MESSAGE");
        when(ollamaService.generateProactiveMessage(anyLong(), eq(50), any()))
                .thenReturn("Just checking in.");
        when(repository.save(any(ConversationMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProactiveMessageService service = new ProactiveMessageService(
                ollamaService, moodService, redisTemplate, identityService, repository);
        ReflectionTestUtils.setField(
                service, "lastInteractionAt", Instant.now().minusSeconds(301));

        service.check();

        var savedMessage = org.mockito.ArgumentCaptor.forClass(ConversationMessage.class);
        verify(repository).save(savedMessage.capture());
        assertEquals(identity, savedMessage.getValue().getIdentity());
        assertEquals("assistant", savedMessage.getValue().getRole());
        assertEquals("Just checking in.", savedMessage.getValue().getContent());
        verify(listOperations).rightPush(
                "conversation", new ChatMessage("assistant", "Just checking in."));
        verify(listOperations).trim("conversation", -200, -1);
    }
}

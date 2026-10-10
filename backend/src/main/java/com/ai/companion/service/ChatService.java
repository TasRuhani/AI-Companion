package com.ai.companion.service;

import com.ai.companion.dto.*;
import com.ai.companion.entity.CompanionIdentity;
import com.ai.companion.entity.CompanionMood;
import com.ai.companion.entity.ConversationMessage;
import com.ai.companion.entity.Memory;
import com.ai.companion.repository.ConversationMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final OllamaService ollamaService;
    private final RedisTemplate<String, ChatMessage> redisTemplate;
    private final MemoryRetrievalService memoryRetrievalService;
    private final MoodService moodService;
    private final WebSearchService webSearchService;
    private final ProactiveMessageService proactiveMessageService;
    private final CompanionIdentityService identityService;
    private final CompanionResetService resetService;
    private final ConversationMessageRepository conversationMessageRepository;
    private final SettingsService settingsService;

    public ChatService(
            OllamaService ollamaService,
            RedisTemplate<String, ChatMessage> redisTemplate,
            MemoryRetrievalService memoryRetrievalService,
            MoodService moodService, WebSearchService webSearchService,
            ProactiveMessageService proactiveMessageService,
            CompanionIdentityService identityService,
            CompanionResetService resetService,
            ConversationMessageRepository conversationMessageRepository,
            SettingsService settingsService) {

        this.ollamaService = ollamaService;
        this.redisTemplate = redisTemplate;
        this.memoryRetrievalService = memoryRetrievalService;
        this.moodService = moodService;
        this.webSearchService = webSearchService;
        this.proactiveMessageService = proactiveMessageService;
        this.identityService = identityService;
        this.resetService = resetService;
        this.conversationMessageRepository = conversationMessageRepository;
        this.settingsService = settingsService;
    }

    public String chat(String message) {
        return resetService.withStableState(
                () -> chatWithStableIdentity(message));
    }

    private String chatWithStableIdentity(String message) {
        CompanionIdentity identity;
        try {
            identity = identityService.getIdentity();
        } catch (ResponseStatusException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Set up a companion identity before starting a conversation.");
        }
        if (settingsService.getProfile() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Set up your user profile before starting a conversation.");
        }

        hydrateRedisConversation(identity);
        ChatMessage userMessage = new ChatMessage(
                "user",
                message
        );
        persist(identity, userMessage);

        redisTemplate.opsForList()
                .rightPush("conversation", userMessage);

        redisTemplate.opsForList()
                .trim("conversation", -200, -1);

        List<ChatMessage> messages = getConversation();

        proactiveMessageService.recordUserInteraction();

        InteractionEvaluation interaction =
                ollamaService.evaluateInteraction(
                        message,
                        messages
                );

        log.debug("Evaluated interaction as {} with confidence {}.",
                interaction.interactionType(), interaction.confidence());

        CompanionMood mood =
                moodService.updateMood(
                        interaction.interactionType(),
                        interaction.confidence()
                );

        log.debug("Updated companion mood score to {}.", mood.getMoodScore());

        List<Memory> relevantMemories =
                memoryRetrievalService.retrieveRelevantMemories(
                        message,
                        5
                );

        ToolDecision toolDecision =
                ollamaService.evaluateToolDecision(
                        message,
                        messages
                );

        log.debug("Selected chat tool action: {}.", toolDecision.action());

        String response;

        if (toolDecision.action() == ToolDecision.Action.WEB_SEARCH) {

            List<WebSearchResult> webResults =
                    webSearchService.search(
                            toolDecision.query(),
                            5
                    );

            log.debug("Web search returned {} results.", webResults.size());

            response =
                    ollamaService.generateWithWebResults(
                            messages,
                            relevantMemories,
                            mood,
                            webResults
                    );

        } else {

            response =
                    ollamaService.generate(
                            messages,
                            relevantMemories,
                            mood
                    );
        }

        try {
            MemoryEvaluation evaluation = ollamaService.evaluateMemory(
                    message, List.of(), messages);
            if (evaluation.worthRemembering()
                    && evaluation.content() != null
                    && !evaluation.content().isBlank()) {
                settingsService.proposeConversationFact(evaluation.content());
            }
        } catch (RestClientException | DataAccessException | IllegalStateException exception) {
            log.warn("Could not extract or save a proposed personal fact; chat response is unaffected.",
                    exception);
        }

        ChatMessage assistantMessage =
                new ChatMessage(
                        "assistant",
                        response
                );
        persist(identity, assistantMessage);

        redisTemplate.opsForList()
                .rightPush(
                        "conversation",
                        assistantMessage
                );

        redisTemplate.opsForList()
                .trim(
                        "conversation",
                        -200,
                        -1
                );

        return response;
    }

    public List<ConversationMessageResponse> getHistory() {
        CompanionIdentity identity = identityService.getIdentity();
        return conversationMessageRepository
                .findByIdentityIdOrderByIdAsc(identity.getId())
                .stream()
                .map(message -> new ConversationMessageResponse(
                        message.getId(),
                        message.getRole(),
                        message.getContent(),
                        message.getCreatedAt()))
                .toList();
    }

    private void persist(CompanionIdentity identity, ChatMessage message) {
        ConversationMessage saved = new ConversationMessage();
        saved.setIdentity(identity);
        saved.setRole(message.role());
        saved.setContent(message.content());
        conversationMessageRepository.save(saved);
    }

    private void hydrateRedisConversation(CompanionIdentity identity) {
        Long historyLength = redisTemplate.opsForList().size("conversation");
        if (historyLength != null && historyLength > 0) {
            return;
        }
        List<ConversationMessage> history = conversationMessageRepository
                .findTop200ByIdentityIdOrderByIdDesc(identity.getId());
        for (int index = history.size() - 1; index >= 0; index--) {
            ConversationMessage item = history.get(index);
            redisTemplate.opsForList().rightPush(
                    "conversation",
                    new ChatMessage(item.getRole(), item.getContent()));
        }
    }

    private List<ChatMessage> getConversation() {

        return redisTemplate.opsForList()
                .range(
                        "conversation",
                        -20,
                        -1
                );
    }
}
package com.ai.companion.service;

import com.ai.companion.dto.ChatMessage;
import com.ai.companion.entity.CompanionIdentity;
import com.ai.companion.entity.CompanionMood;
import com.ai.companion.entity.ConversationMessage;
import com.ai.companion.entity.InteractionType;
import com.ai.companion.repository.ConversationMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class ProactiveMessageService {

    private static final Logger log = LoggerFactory.getLogger(ProactiveMessageService.class);
    private static final long PROACTIVE_RETRY_SECONDS = 600;

    private final OllamaService ollamaService;
    private final MoodService moodService;
    private final RedisTemplate<String, ChatMessage> redisTemplate;
    private final CompanionIdentityService identityService;
    private final ConversationMessageRepository conversationMessageRepository;

    private Instant lastInteractionAt;
    private Instant lastProactiveMessageAt;

    private boolean proactiveMessagePending = false;
    private boolean checkInProgress = false;

    public ProactiveMessageService(
            OllamaService ollamaService,
            MoodService moodService,
            RedisTemplate<String, ChatMessage> redisTemplate,
            CompanionIdentityService identityService,
            ConversationMessageRepository conversationMessageRepository) {

        this.ollamaService = ollamaService;
        this.moodService = moodService;
        this.redisTemplate = redisTemplate;
        this.identityService = identityService;
        this.conversationMessageRepository = conversationMessageRepository;
    }

    public synchronized void check() {

        if (!identityService.exists()) {
            resetTracking();
            return;
        }

        if (checkInProgress) {
            log.debug("Proactive check already running; skipping.");
            return;
        }

        checkInProgress = true;

        try {

            if (lastInteractionAt == null) {
                log.debug("No user interaction recorded yet; skipping proactive check.");
                return;
            }

            CompanionMood mood = moodService.getMood();

            List<ChatMessage> messages =
                    redisTemplate.opsForList()
                            .range("conversation", -20, -1);

            long secondsSinceLastInteraction =
                    Duration.between(
                            lastInteractionAt,
                            Instant.now()
                    ).getSeconds();

            if (secondsSinceLastInteraction < 300) {
                log.debug("Skipping proactive message; last interaction was {} seconds ago.",
                        secondsSinceLastInteraction);
                return;
            }

            if (proactiveMessagePending) {

                long secondsSinceProactiveMessage =
                        Duration.between(
                                lastProactiveMessageAt,
                                Instant.now()
                        ).getSeconds();

                if (secondsSinceProactiveMessage
                        < PROACTIVE_RETRY_SECONDS) {

                    log.debug("Waiting for a response to the proactive message; {} seconds elapsed.",
                            secondsSinceProactiveMessage);

                    return;
                }
            }

            String decision =
                    ollamaService.evaluateProactiveMessage(
                            secondsSinceLastInteraction,
                            mood.getMoodScore(),
                            messages
                    );

            log.debug("Proactive-message decision: {} ({} seconds since last interaction).",
                    decision, secondsSinceLastInteraction);

            if (decision.equalsIgnoreCase("MESSAGE")) {

                if (proactiveMessagePending) {

                    log.info("Previous proactive message was ignored.");

                    moodService.updateMood(
                            InteractionType.IGNORED,
                            1.0
                    );

                    mood = moodService.getMood();

                    log.debug("Mood after ignored proactive message: {}.", mood.getMoodScore());
                }

                String content = ollamaService.generateProactiveMessage(
                        secondsSinceLastInteraction,
                        mood.getMoodScore(),
                        messages
                );
                CompanionIdentity identity = identityService.getIdentity();
                ConversationMessage proactiveMessage = new ConversationMessage();
                proactiveMessage.setIdentity(identity);
                proactiveMessage.setRole("assistant");
                proactiveMessage.setContent(content);
                conversationMessageRepository.save(proactiveMessage);

                lastProactiveMessageAt = Instant.now();
                proactiveMessagePending = true;
                ChatMessage chatMessage = new ChatMessage("assistant", content);
                redisTemplate.opsForList().rightPush("conversation", chatMessage);
                redisTemplate.opsForList().trim("conversation", -200, -1);

                log.info("Generated and stored a proactive message for identity {}.", identity.getId());
            }

        } finally {

            checkInProgress = false;
        }
    }

    public synchronized void recordUserInteraction() {

        lastInteractionAt = Instant.now();


        if (proactiveMessagePending) {

            proactiveMessagePending = false;

            log.debug("User responded to the pending proactive message.");
        }

        log.debug("Recorded user interaction at {}.", lastInteractionAt);
    }

    public synchronized void resetTracking() {
        lastInteractionAt = null;
        lastProactiveMessageAt = null;
        proactiveMessagePending = false;
    }

    public synchronized void resetTrackingAfter(Runnable persistentReset) {
        persistentReset.run();
        resetTracking();
    }
}
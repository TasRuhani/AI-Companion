package com.ai.companion.service;

import com.ai.companion.dto.ChatMessage;
import com.ai.companion.entity.CompanionMood;
import com.ai.companion.entity.InteractionType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class ProactiveMessageService {

    private static final long PROACTIVE_RETRY_SECONDS = 600;

    private final OllamaService ollamaService;
    private final MoodService moodService;
    private final RedisTemplate<String, ChatMessage> redisTemplate;

    private Instant lastInteractionAt;
    private Instant lastProactiveMessageAt;

    private boolean proactiveMessagePending = false;
    private boolean checkInProgress = false;

    public ProactiveMessageService(
            OllamaService ollamaService,
            MoodService moodService,
            RedisTemplate<String, ChatMessage> redisTemplate) {

        this.ollamaService = ollamaService;
        this.moodService = moodService;
        this.redisTemplate = redisTemplate;
    }

    public void check() {

        if (checkInProgress) {
            System.out.println(
                    "Proactive check already running. Skipping."
            );
            return;
        }

        checkInProgress = true;

        try {

            if (lastInteractionAt == null) {
                System.out.println(
                        "No user interaction recorded yet. "
                                + "Skipping proactive check."
                );
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
                System.out.println(
                        "Too soon to proactively message."
                );
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

                    System.out.println(
                            "Waiting for response to proactive message. "
                                    + "Seconds elapsed: "
                                    + secondsSinceProactiveMessage
                    );

                    return;
                }
            }

            String decision =
                    ollamaService.evaluateProactiveMessage(
                            secondsSinceLastInteraction,
                            mood.getMoodScore(),
                            messages
                    );

            System.out.println(
                    "Seconds since last interaction: "
                            + secondsSinceLastInteraction
            );

            System.out.println(
                    "Proactive decision: " + decision
            );

            if (decision.equalsIgnoreCase("MESSAGE")) {

                if (proactiveMessagePending) {

                    System.out.println(
                            "Previous proactive message was ignored."
                    );

                    moodService.updateMood(
                            InteractionType.IGNORED,
                            1.0
                    );

                    mood = moodService.getMood();

                    System.out.println(
                            "Mood after ignored proactive message: "
                                    + mood.getMoodScore()
                    );
                }

                String proactiveMessage =
                        ollamaService.generateProactiveMessage(
                                secondsSinceLastInteraction,
                                mood.getMoodScore(),
                                messages
                        );

                System.out.println(
                        "PROACTIVE MESSAGE: " + proactiveMessage
                );

                lastProactiveMessageAt = Instant.now();
                proactiveMessagePending = true;
            }

        } finally {

            checkInProgress = false;
        }
    }

    public void recordUserInteraction() {

        lastInteractionAt = Instant.now();


        if (proactiveMessagePending) {

            proactiveMessagePending = false;

            System.out.println(
                    "USER RESPONDED TO PROACTIVE MESSAGE"
            );
        }

        System.out.println(
                "USER INTERACTION RECORDED AT: "
                        + lastInteractionAt
        );
    }
}
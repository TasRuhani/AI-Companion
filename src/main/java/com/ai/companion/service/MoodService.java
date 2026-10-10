package com.ai.companion.service;

import com.ai.companion.entity.CompanionMood;
import com.ai.companion.entity.CompanionProfile;
import com.ai.companion.entity.InteractionType;
import com.ai.companion.entity.MoodEvent;
import com.ai.companion.repository.CompanionMoodRepository;
import com.ai.companion.repository.CompanionProfileRepository;
import com.ai.companion.repository.MoodEventRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class MoodService {

    private final CompanionMoodRepository moodRepository;
    private final MoodEventRepository moodEventRepository;
    private final CompanionProfileRepository profileRepository;

    private CompanionProfile getActiveProfile() {
        return profileRepository.findByActiveTrue()
                .orElse(null);
    }

    public MoodService(
            CompanionMoodRepository moodRepository,
            MoodEventRepository moodEventRepository,
            CompanionProfileRepository profileRepository) {

        this.moodRepository = moodRepository;
        this.moodEventRepository = moodEventRepository;
        this.profileRepository = profileRepository;
    }

    public CompanionMood getMood() {

        return moodRepository.findAll()
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    CompanionMood mood = new CompanionMood();

                    CompanionProfile profile =
                            profileRepository.findByActiveTrue()
                                    .orElse(null);

                    if (profile != null) {
                        mood.setBaselineMood(
                                profile.getBaselineMood()
                        );

                        mood.setMoodScore(
                                profile.getBaselineMood()
                        );
                    }

                    return moodRepository.save(mood);
                });
    }

    public CompanionMood updateMood(
            InteractionType interactionType,
            double confidence) {

        CompanionMood mood = getMood();

        Instant now = Instant.now();

        if (isRapidFire(mood, now)) {

            System.out.println(
                    "MOOD UPDATE SKIPPED: interaction arrived too quickly."
            );

            return mood;
        }

        applyTimeRecovery(mood, now);

        int baseDelta =
                getDelta(interactionType);

        double confidenceMultiplier =
                Math.max(
                        0.0,
                        Math.min(1.0, confidence)
                );

        CompanionProfile profile = getActiveProfile();

        double reactivityMultiplier = 1.0;

        if (profile != null) {
            reactivityMultiplier =
                    profile.getReactivity() / 50.0;
        }

        double confidenceAdjustedDelta =
                baseDelta
                        * confidenceMultiplier
                        * reactivityMultiplier;
        int consecutiveCount;

        if (baseDelta > 0) {

            consecutiveCount =
                    mood.getConsecutivePositiveCount();

        } else if (baseDelta < 0) {

            consecutiveCount =
                    mood.getConsecutiveNegativeCount();

        } else {

            consecutiveCount = 0;
        }

        double dampingFactor =
                1.0 + (consecutiveCount * 0.25);

        double dampedDelta =
                confidenceAdjustedDelta
                        / dampingFactor;

        double boundaryMultiplier =
                getBoundaryMultiplier(
                        mood.getMoodScore(),
                        dampedDelta
                );

        int effectiveDelta =
                (int) Math.round(
                        dampedDelta
                                * boundaryMultiplier
                );

        int oldScore =
                mood.getMoodScore();

        int newScore =
                oldScore + effectiveDelta;

        newScore =
                Math.max(
                        -100,
                        Math.min(100, newScore)
                );

        mood.setMoodScore(newScore);

        double direction;

        if (baseDelta > 0) {

            direction = 1.0;

        } else if (baseDelta < 0) {

            direction = -1.0;

        } else {

            direction = 0.0;
        }

        double newTrend =
                mood.getRecentTrend() * 0.7
                        + direction * 0.3;

        mood.setRecentTrend(newTrend);

        if (baseDelta > 0) {

            mood.setConsecutivePositiveCount(
                    mood.getConsecutivePositiveCount() + 1
            );

            mood.setConsecutiveNegativeCount(0);

        } else if (baseDelta < 0) {

            mood.setConsecutiveNegativeCount(
                    mood.getConsecutiveNegativeCount() + 1
            );

            mood.setConsecutivePositiveCount(0);

        } else {

            mood.setConsecutivePositiveCount(0);
            mood.setConsecutiveNegativeCount(0);
        }

        mood.setLastInteractionType(
                interactionType
        );

        mood.setLastInteractionAt(now);
        CompanionMood savedMood =
                moodRepository.save(mood);

        MoodEvent event = new MoodEvent();

        event.setInteractionType(
                interactionType
        );

        event.setConfidence(
                confidence
        );

        event.setOldMoodScore(
                oldScore
        );

        event.setDelta(
                effectiveDelta
        );

        event.setNewMoodScore(
                newScore
        );

        moodEventRepository.save(event);

        System.out.println(
                "MOOD UPDATE:"
        );

        System.out.println(
                "  Interaction: "
                        + interactionType
        );

        System.out.println(
                "  Confidence: "
                        + confidence
        );

        System.out.println(
                "  Old score: "
                        + oldScore
        );

        System.out.println(
                "  Base delta: "
                        + baseDelta
        );

        System.out.println(
                "  Confidence multiplier: "
                        + confidenceMultiplier
        );

        System.out.println(
                "  Damping factor: "
                        + dampingFactor
        );

        System.out.println(
                "  Boundary multiplier: "
                        + boundaryMultiplier
        );

        System.out.println(
                "  Effective delta: "
                        + effectiveDelta
        );

        System.out.println(
                "  New score: "
                        + newScore
        );

        System.out.println(
                "  Recent trend: "
                        + newTrend
        );

        return savedMood;
    }

    private boolean isRapidFire(
            CompanionMood mood,
            Instant now) {

        Instant lastInteraction =
                mood.getLastInteractionAt();

        if (lastInteraction == null) {
            return false;
        }

        long milliseconds =
                Duration.between(
                        lastInteraction,
                        now
                ).toMillis();

        return milliseconds < 2000;
    }
    private int getDelta(
            InteractionType interactionType) {

        return switch (interactionType) {

            case IGNORED -> -5;

            case MINIMAL -> -2;

            case NORMAL -> 0;

            case ENGAGED -> 5;

            case AFFECTIONATE -> 8;

            case DISMISSIVE -> -8;
        };
    }
    private void applyTimeRecovery(
            CompanionMood mood,
            Instant now) {

        Instant lastInteraction =
                mood.getLastInteractionAt();

        if (lastInteraction == null) {
            return;
        }

        long hours =
                Duration.between(
                        lastInteraction,
                        now
                ).toHours();

        if (hours < 1) {
            return;
        }

        int currentScore =
                mood.getMoodScore();

        int baseline =
                mood.getBaselineMood();

        if (currentScore == baseline) {
            return;
        }

        CompanionProfile profile = getActiveProfile();

        double forgivenessMultiplier = 1.0;

        if (profile != null) {
            forgivenessMultiplier =
                    profile.getForgiveness() / 50.0;
        }

        int recovery =
                (int) Math.max(
                        1,
                        Math.round(
                                Math.min(hours, 5)
                                        * forgivenessMultiplier
                        )
                );

        if (currentScore > baseline) {

            currentScore =
                    Math.max(
                            baseline,
                            currentScore - recovery
                    );

        } else {

            currentScore =
                    Math.min(
                            baseline,
                            currentScore + recovery
                    );
        }

        mood.setMoodScore(currentScore);

        double trend =
                mood.getRecentTrend();

        double decay =
                Math.pow(
                        0.8,
                        hours
                );

        mood.setRecentTrend(
                trend * decay
        );
    }
    private double getBoundaryMultiplier(
            int moodScore,
            double delta) {

        if (delta > 0) {

            if (moodScore >= 90) {
                return 0.25;
            }

            if (moodScore >= 75) {
                return 0.50;
            }

            if (moodScore >= 50) {
                return 0.75;
            }

            return 1.0;
        }

        if (delta < 0) {

            if (moodScore <= -90) {
                return 0.25;
            }

            if (moodScore <= -75) {
                return 0.50;
            }

            if (moodScore <= -50) {
                return 0.75;
            }

            return 1.0;
        }

        return 1.0;
    }
}
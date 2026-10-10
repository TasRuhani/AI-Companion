package com.ai.companion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "companion_mood")
@Getter
@Setter
@NoArgsConstructor
public class CompanionMood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Mood range:
     * -100 = worst possible mood
     *    0 = normal
     * +100 = best/loving mood
     */
    @Column(nullable = false)
    private int moodScore = 0;

    /*
     * Allows us to eventually have a naturally warmer/cooler personality.
     * Keep 0 for now.
     */
    @Column(nullable = false)
    private int baselineMood = 0;

    @Enumerated(EnumType.STRING)
    private InteractionType lastInteractionType;

    private Instant lastInteractionAt;

    @Column(nullable = false)
    private int consecutiveNegativeCount = 0;

    @Column(nullable = false)
    private int consecutivePositiveCount = 0;

    /*
     * Represents the recent direction of the mood.
     * Positive = mood has generally been improving.
     * Negative = mood has generally been getting worse.
     */
    @Column(nullable = false)
    private double recentTrend = 0.0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    /*
     * Protects against two simultaneous mood updates
     * overwriting each other.
     */
    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
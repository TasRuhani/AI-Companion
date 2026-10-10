package com.ai.companion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "mood_events")
@Getter
@Setter
@NoArgsConstructor
public class MoodEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InteractionType interactionType;

    @Column(nullable = false)
    private double confidence;

    @Column(nullable = false)
    private int oldMoodScore;

    @Column(nullable = false)
    private int delta;

    @Column(nullable = false)
    private int newMoodScore;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
package com.ai.companion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "companion_profiles")
@Getter
@Setter
@NoArgsConstructor
public class CompanionProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String corePersona;

    @Column(columnDefinition = "TEXT")
    private String speechStyle;

    @Column(nullable = false)
    private int warmth = 50;

    @Column(nullable = false)
    private int reactivity = 50;

    @Column(nullable = false)
    private int forgiveness = 50;

    @Column(nullable = false)
    private int baselineMood = 0;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private int profileVersion = 1;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
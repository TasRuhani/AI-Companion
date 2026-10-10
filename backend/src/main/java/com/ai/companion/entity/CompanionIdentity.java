package com.ai.companion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "companion_identity")
@Getter
@Setter
@NoArgsConstructor
public class CompanionIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String personality;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String background;


    @Column(nullable = false, columnDefinition = "TEXT")
    private String history;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String speechStyle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String values;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IdentitySource source;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 36)
    private String portraitId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

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
package com.ai.companion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "profile_facts")
@Getter
@Setter
@NoArgsConstructor
public class ProfileFact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private CompanionProfile profile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FactType type;


    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;


    @Column(columnDefinition = "TEXT")
    private String sourceUrl;

    private Instant sourceRetrievedAt;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(
            name = "embedding",
            columnDefinition = "vector(1024)"
    )
    private float[] embedding;
}
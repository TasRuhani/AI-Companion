package com.ai.companion.dto;

import com.ai.companion.entity.PersonalFactStatus;

import java.time.Instant;

public record PersonalFactResponse(
        Long id,
        String content,
        PersonalFactStatus status,
        String source,
        String reviewReason,
        Instant createdAt,
        Instant updatedAt
) {
}

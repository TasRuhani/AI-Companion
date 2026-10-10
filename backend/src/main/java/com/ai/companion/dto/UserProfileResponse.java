package com.ai.companion.dto;

import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String name,
        String preferredNickname,
        Integer age,
        String gender,
        String bio,
        String interests,
        String favoriteTopics,
        String communicationPreferences,
        String thingsToKnow,
        String topicsToAvoid,
        Instant createdAt,
        Instant updatedAt
) {
}

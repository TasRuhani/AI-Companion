package com.ai.companion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserProfileRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 120) String preferredNickname,
        @Min(1) @Max(130) Integer age,
        @Size(max = 120) String gender,
        @Size(max = 5000) String bio,
        @Size(max = 5000) String interests,
        @Size(max = 5000) String favoriteTopics,
        @Size(max = 5000) String communicationPreferences,
        @Size(max = 5000) String thingsToKnow,
        @Size(max = 5000) String topicsToAvoid
) {
}

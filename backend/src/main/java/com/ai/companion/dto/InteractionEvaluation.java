package com.ai.companion.dto;

import com.ai.companion.entity.InteractionType;

public record InteractionEvaluation(
        InteractionType interactionType,
        double confidence
) {
}
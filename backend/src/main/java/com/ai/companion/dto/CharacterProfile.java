package com.ai.companion.dto;

public record CharacterProfile(
        String name,
        String personality,
        String background,
        String history,
        String speechStyle,
        String values,
        String relationships
) {
}
package com.ai.companion.dto;

public record WebSearchResult(
        String title,
        String url,
        String snippet
) {}
package com.ai.companion.dto;

public record ToolDecision(
        Action action,
        String query
) {

    public enum Action {
        ANSWER,
        WEB_SEARCH
    }
}
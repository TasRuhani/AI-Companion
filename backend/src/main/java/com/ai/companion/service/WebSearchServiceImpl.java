package com.ai.companion.service;

import com.ai.companion.dto.WebSearchResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class WebSearchServiceImpl implements WebSearchService {

    private final RestClient client;
    private final String apiKey;
    private final boolean enabled;

    public WebSearchServiceImpl(
            @Value("${companion.web-search.api-key:}") String apiKey,
            @Value("${companion.web-search.enabled:false}") boolean enabled) {

        this.apiKey = apiKey;
        this.enabled = enabled;

        this.client = RestClient.builder()
                .baseUrl("https://api.tavily.com")
                .build();
    }

    @Override
    public List<WebSearchResult> search(
            String query,
            int limit) {

        if (!enabled) {
            return List.of();
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Web search is enabled but the API key is not configured"
            );
        }

        if (query == null || query.isBlank()) {
            return List.of();
        }

        int safeLimit =
                Math.max(1, Math.min(limit, 10));

        Map<String, Object> requestBody =
                Map.of(
                        "query", query,
                        "search_depth", "basic",
                        "max_results", safeLimit,
                        "include_answer", false
                );

        TavilySearchResponse response =
                client.post()
                        .uri("/search")
                        .header(
                                "Authorization",
                                "Bearer " + apiKey
                        )
                        .body(requestBody)
                        .retrieve()
                        .body(TavilySearchResponse.class);

        if (response == null || response.results() == null) {
            return List.of();
        }

        return response.results()
                .stream()
                .map(result ->
                        new WebSearchResult(
                                result.title(),
                                result.url(),
                                result.content()
                        )
                )
                .toList();
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private record TavilySearchResponse(
            List<TavilySearchItem> results
    ) {}

    private record TavilySearchItem(
            String title,
            String url,
            String content
    ) {}
}
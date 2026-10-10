package com.ai.companion.service;

import com.ai.companion.dto.WebSearchResult;

import java.util.List;

public interface WebSearchService {

    List<WebSearchResult> search(
            String query,
            int limit
    );
}
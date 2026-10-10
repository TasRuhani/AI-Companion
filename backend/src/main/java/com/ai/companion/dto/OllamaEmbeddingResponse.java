package com.ai.companion.dto;

import java.util.List;

public record OllamaEmbeddingResponse (
        List<List<Float>> embeddings
){}

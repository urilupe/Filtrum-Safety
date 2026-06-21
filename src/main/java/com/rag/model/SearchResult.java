package com.rag.model;

public record SearchResult(
        DocumentChunk chunk,
        double score
) {}

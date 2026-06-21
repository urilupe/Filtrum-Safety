package com.rag.model;

public record DocumentSummary(
        String documentId,
        String fileName,
        int chunkCount
) {}

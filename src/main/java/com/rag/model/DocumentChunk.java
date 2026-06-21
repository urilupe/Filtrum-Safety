package com.rag.model;

import java.util.Map;

public record DocumentChunk(
        String id,
        String documentId,
        String fileName,
        int chunkIndex,
        String content,
        float[] embedding,
        Map<String, String> metadata
) {}

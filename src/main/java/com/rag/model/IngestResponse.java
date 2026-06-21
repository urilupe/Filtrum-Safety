package com.rag.model;

public record IngestResponse(
        String documentId,
        String fileName,
        int chunksCreated
) {}

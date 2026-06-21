package com.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public record RagProperties(
        ChunkProperties chunk,
        SearchProperties search,
        StorageProperties storage
) {
    public record ChunkProperties(int size, int overlap) {}
    public record SearchProperties(int defaultTopK, double defaultMinScore) {}
    public record StorageProperties(String dataDir, String uploadsDir) {}
}

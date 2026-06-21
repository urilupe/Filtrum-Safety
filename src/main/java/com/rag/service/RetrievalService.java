package com.rag.service;

import com.rag.config.RagProperties;
import com.rag.model.SearchResult;
import com.rag.store.InMemoryVectorStore;
import com.rag.util.CosineSimilarity;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class RetrievalService {

    private final EmbeddingService embeddingService;
    private final InMemoryVectorStore vectorStore;
    private final RagProperties properties;

    public RetrievalService(
            EmbeddingService embeddingService,
            InMemoryVectorStore vectorStore,
            RagProperties properties
    ) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.properties = properties;
    }

    public List<SearchResult> search(String query, int topK, double minScore) {
        if (vectorStore.count() == 0) {
            return List.of();
        }

        float[] queryEmbedding = embeddingService.embed(query);

        return vectorStore.findAll().stream()
                .map(chunk -> new SearchResult(
                        chunk,
                        CosineSimilarity.compute(queryEmbedding, chunk.embedding())
                ))
                .filter(result -> result.score() >= minScore)
                .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
                .limit(topK)
                .toList();
    }

    public List<SearchResult> searchWithDefaults(String query) {
        return search(
                query,
                properties.search().defaultTopK(),
                properties.search().defaultMinScore()
        );
    }
}

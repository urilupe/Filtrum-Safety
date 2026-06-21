package com.rag.store;

import com.rag.model.DocumentChunk;
import com.rag.model.DocumentSummary;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Almacén en memoria con índice por documento.
 * Interfaz simple para reemplazar luego por Qdrant, pgvector u otro motor vectorial.
 */
@Repository
public class InMemoryVectorStore {

    private final Map<String, DocumentChunk> chunksById = new ConcurrentHashMap<>();

    public void upsert(DocumentChunk chunk) {
        chunksById.put(chunk.id(), chunk);
    }

    public void upsertAll(List<DocumentChunk> chunks) {
        chunks.forEach(this::upsert);
    }

    public List<DocumentChunk> findAll() {
        return new ArrayList<>(chunksById.values());
    }

    public List<DocumentChunk> findByDocumentId(String documentId) {
        return chunksById.values().stream()
                .filter(chunk -> chunk.documentId().equals(documentId))
                .sorted(Comparator.comparingInt(DocumentChunk::chunkIndex))
                .toList();
    }

    public void deleteByDocumentId(String documentId) {
        chunksById.entrySet().removeIf(entry -> entry.getValue().documentId().equals(documentId));
    }

    public int count() {
        return chunksById.size();
    }

    public List<DocumentSummary> listDocuments() {
        return chunksById.values().stream()
                .collect(Collectors.groupingBy(DocumentChunk::documentId))
                .entrySet().stream()
                .map(entry -> {
                    List<DocumentChunk> chunks = entry.getValue();
                    DocumentChunk first = chunks.getFirst();
                    return new DocumentSummary(
                            entry.getKey(),
                            first.fileName(),
                            chunks.size()
                    );
                })
                .sorted(Comparator.comparing(DocumentSummary::fileName))
                .toList();
    }
}

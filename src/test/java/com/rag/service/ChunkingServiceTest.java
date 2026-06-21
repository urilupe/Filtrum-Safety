package com.rag.service;

import com.rag.config.RagProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkingServiceTest {

    private final ChunkingService chunkingService = new ChunkingService(
            new RagProperties(
                    new RagProperties.ChunkProperties(50, 10),
                    new RagProperties.SearchProperties(5, 0.55),
                    new RagProperties.StorageProperties("./data", "./data/uploads")
            )
    );

    @Test
    void splitsTextIntoNonEmptyChunks() {
        String text = "Primera oración. Segunda oración. Tercera oración con más contenido.";
        List<String> chunks = chunkingService.split(text);

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.stream().allMatch(chunk -> !chunk.isBlank()));
    }
}

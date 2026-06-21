package com.rag.service;

import com.rag.config.RagProperties;
import com.rag.model.DocumentChunk;
import com.rag.model.IngestResponse;
import com.rag.store.InMemoryVectorStore;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class IngestionService {

    private final Tika tika = new Tika();
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final InMemoryVectorStore vectorStore;
    private final Path uploadsDir;

    public IngestionService(
            ChunkingService chunkingService,
            EmbeddingService embeddingService,
            InMemoryVectorStore vectorStore,
            RagProperties properties
    ) throws IOException {
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.uploadsDir = Path.of(properties.storage().uploadsDir());
        Files.createDirectories(uploadsDir);
    }

    public IngestResponse ingest(MultipartFile file) throws Exception {
        String documentId = UUID.randomUUID().toString();
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "documento.txt";

        Path savedFile = uploadsDir.resolve(documentId + "_" + fileName);
        file.transferTo(savedFile);

        String text = tika.parseToString(savedFile);
        List<String> chunks = chunkingService.split(text);

        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("No se extrajo contenido del archivo: " + fileName);
        }

        List<DocumentChunk> documentChunks = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String content = chunks.get(i);
            float[] embedding = embeddingService.embed(content);

            documentChunks.add(new DocumentChunk(
                    UUID.randomUUID().toString(),
                    documentId,
                    fileName,
                    i,
                    content,
                    embedding,
                    Map.of(
                            "source", fileName,
                            "documentId", documentId,
                            "savedPath", savedFile.toString()
                    )
            ));
        }

        vectorStore.upsertAll(documentChunks);
        return new IngestResponse(documentId, fileName, documentChunks.size());
    }

    public List<IngestResponse> ingestAll(MultipartFile[] files) throws Exception {
        List<IngestResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            responses.add(ingest(file));
        }
        return responses;
    }
}

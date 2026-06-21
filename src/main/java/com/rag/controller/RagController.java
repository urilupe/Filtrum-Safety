package com.rag.controller;

import com.rag.config.RagProperties;
import com.rag.model.DocumentSummary;
import com.rag.model.IngestResponse;
import com.rag.model.SearchRequest;
import com.rag.model.SearchResult;
import com.rag.model.SearchResultResponse;
import com.rag.service.IngestionService;
import com.rag.service.MarkdownExportService;
import com.rag.service.RetrievalService;
import com.rag.store.InMemoryVectorStore;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final IngestionService ingestionService;
    private final RetrievalService retrievalService;
    private final MarkdownExportService markdownExportService;
    private final InMemoryVectorStore vectorStore;
    private final RagProperties properties;

    public RagController(
            IngestionService ingestionService,
            RetrievalService retrievalService,
            MarkdownExportService markdownExportService,
            InMemoryVectorStore vectorStore,
            RagProperties properties
    ) {
        this.ingestionService = ingestionService;
        this.retrievalService = retrievalService;
        this.markdownExportService = markdownExportService;
        this.vectorStore = vectorStore;
        this.properties = properties;
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<IngestResponse>> uploadDocuments(
            @RequestParam("files") MultipartFile[] files
    ) throws Exception {
        List<IngestResponse> responses = ingestionService.ingestAll(files);
        return ResponseEntity.accepted().body(responses);
    }

    @GetMapping("/documents")
    public List<DocumentSummary> listDocuments() {
        return vectorStore.listDocuments();
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String documentId) {
        vectorStore.deleteByDocumentId(documentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/search")
    public List<SearchResultResponse> search(@Valid @RequestBody SearchRequest request) {
        return retrievalService.search(
                request.query(),
                request.resolvedTopK(properties.search().defaultTopK()),
                request.resolvedMinScore(properties.search().defaultMinScore())
        ).stream().map(SearchResultResponse::from).toList();
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> exportMarkdown(@Valid @RequestBody SearchRequest request) {
        List<SearchResult> results = retrievalService.search(
                request.query(),
                request.resolvedTopK(properties.search().defaultTopK()),
                request.resolvedMinScore(properties.search().defaultMinScore())
        );

        String markdown = markdownExportService.buildContextFile(request.query(), results);
        byte[] bytes = markdown.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rag-context.md\"")
                .contentType(MediaType.parseMediaType("text/markdown; charset=UTF-8"))
                .body(bytes);
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "chunksIndexed", vectorStore.count(),
                "documents", vectorStore.listDocuments().size()
        );
    }
}

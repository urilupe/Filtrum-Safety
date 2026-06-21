package com.rag.model;

public record SearchResultResponse(
        String id,
        String documentId,
        String fileName,
        int chunkIndex,
        String content,
        double score
) {
    public static SearchResultResponse from(SearchResult result) {
        return new SearchResultResponse(
                result.chunk().id(),
                result.chunk().documentId(),
                result.chunk().fileName(),
                result.chunk().chunkIndex(),
                result.chunk().content(),
                result.score()
        );
    }
}

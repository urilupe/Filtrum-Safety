package com.rag.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record SearchRequest(
        @NotBlank String query,
        @Positive Integer topK,
        Double minScore
) {
    public int resolvedTopK(int defaultTopK) {
        return topK != null ? topK : defaultTopK;
    }

    public double resolvedMinScore(double defaultMinScore) {
        return minScore != null ? minScore : defaultMinScore;
    }
}

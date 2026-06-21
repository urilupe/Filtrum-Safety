package com.rag.util;

public final class CosineSimilarity {

    private CosineSimilarity() {}

    public static double compute(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Los vectores deben tener la misma dimensión");
        }

        double dot = 0;
        double normA = 0;
        double normB = 0;

        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}

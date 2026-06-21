package com.rag.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CosineSimilarityTest {

    @Test
    void identicalVectorsHaveSimilarityOne() {
        float[] vector = {1f, 0f, 0f};
        assertEquals(1.0, CosineSimilarity.compute(vector, vector), 1e-6);
    }

    @Test
    void orthogonalVectorsHaveSimilarityZero() {
        float[] a = {1f, 0f};
        float[] b = {0f, 1f};
        assertEquals(0.0, CosineSimilarity.compute(a, b), 1e-6);
    }
}

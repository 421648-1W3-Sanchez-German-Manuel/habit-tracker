package com.tp1.habittracker.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class SimilarityUtilsTest {

    @Test
    void cosineSimilarityReturnsOneForIdenticalVectors() {
        double result = SimilarityUtils.cosineSimilarity(
                List.of(1.0, 2.0, 3.0),
                List.of(1.0, 2.0, 3.0)
        );

        assertEquals(1.0, result, 1e-12);
    }

    @Test
    void cosineSimilarityReturnsZeroForOrthogonalVectors() {
        double result = SimilarityUtils.cosineSimilarity(
                List.of(1.0, 0.0),
                List.of(0.0, 1.0)
        );

        assertEquals(0.0, result, 1e-12);
    }

    @Test
    void cosineSimilarityThrowsWhenDifferentSizes() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SimilarityUtils.cosineSimilarity(List.of(1.0, 2.0), List.of(1.0))
        );
    }

    @Test
    void cosineSimilarityThrowsWhenVectorsAreEmpty() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SimilarityUtils.cosineSimilarity(List.of(), List.of())
        );
    }

    @Test
    void cosineSimilarityThrowsWhenMagnitudeIsZero() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SimilarityUtils.cosineSimilarity(List.of(0.0, 0.0), List.of(1.0, 2.0))
        );
    }
}
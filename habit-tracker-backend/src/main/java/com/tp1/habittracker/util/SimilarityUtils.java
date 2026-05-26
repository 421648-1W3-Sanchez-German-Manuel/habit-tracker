package com.tp1.habittracker.util;

import java.util.List;
import java.util.Objects;

public final class SimilarityUtils {

    private SimilarityUtils() {
    }

    public static double cosineSimilarity(List<Double> v1, List<Double> v2) {
        Objects.requireNonNull(v1, "v1 must not be null");
        Objects.requireNonNull(v2, "v2 must not be null");

        if (v1.isEmpty() || v2.isEmpty()) {
            throw new IllegalArgumentException("Vectors must not be empty");
        }

        if (v1.size() != v2.size()) {
            throw new IllegalArgumentException(
                    "Vectors must have the same size: v1=" + v1.size() + ", v2=" + v2.size()
            );
        }

        double dotProduct = 0.0;
        double magnitudeV1Squared = 0.0;
        double magnitudeV2Squared = 0.0;

        for (int i = 0; i < v1.size(); i++) {
            Double a = v1.get(i);
            Double b = v2.get(i);

            if (a == null || b == null) {
                throw new IllegalArgumentException("Vectors must not contain null values");
            }

            dotProduct += a * b;
            magnitudeV1Squared += a * a;
            magnitudeV2Squared += b * b;
        }

        double magnitudeV1 = Math.sqrt(magnitudeV1Squared);
        double magnitudeV2 = Math.sqrt(magnitudeV2Squared);

        if (magnitudeV1 == 0.0 || magnitudeV2 == 0.0) {
            throw new IllegalArgumentException("Vectors with zero magnitude are not supported");
        }

        return dotProduct / (magnitudeV1 * magnitudeV2);
    }
}
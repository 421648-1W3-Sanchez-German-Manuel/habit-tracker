package com.tp1.habittracker.dto.recommendation;

import com.tp1.habittracker.domain.enums.Frequency;
import com.tp1.habittracker.domain.enums.HabitType;

public record HabitRecommendationResponse(
        String habitId,
        String habitName,
        HabitType habitType,
        Frequency habitFrequency,
        String ownerUserId,
        String ownerUsername,
        int graphDistance,
        double similarityScore,
        double weightedScore,
        ClosestOwnHabit closestOwnHabit
) {
    public record ClosestOwnHabit(String id, String name) {
    }
}

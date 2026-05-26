package com.tp1.habittracker.dto.habit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CheckSimilarityRequest(
        @NotBlank(message = "Habit name is required")
        @Size(min = 2, max = 100, message = "Habit name must be between 2 and 100 characters")
        String name
) {
}

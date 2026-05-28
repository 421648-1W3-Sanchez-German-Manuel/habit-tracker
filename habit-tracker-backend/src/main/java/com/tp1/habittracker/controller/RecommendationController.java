package com.tp1.habittracker.controller;

import com.tp1.habittracker.dto.recommendation.HabitRecommendationResponse;
import com.tp1.habittracker.service.HabitRecommendationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final HabitRecommendationService habitRecommendationService;

    @GetMapping("/habits")
    public List<HabitRecommendationResponse> recommendHabits(Authentication authentication) {
        return habitRecommendationService.recommendForUser(extractUserId(authentication));
    }

    private String extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        return authentication.getName();
    }
}

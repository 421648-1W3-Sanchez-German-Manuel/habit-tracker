package com.tp1.habittracker.service;

import com.tp1.habittracker.config.HabitRecommendationProperties;
import com.tp1.habittracker.domain.model.Habit;
import com.tp1.habittracker.domain.model.User;
import com.tp1.habittracker.dto.recommendation.HabitRecommendationResponse;
import com.tp1.habittracker.dto.recommendation.HabitRecommendationResponse.ClosestOwnHabit;
import com.tp1.habittracker.exception.ResourceNotFoundException;
import com.tp1.habittracker.repository.HabitRepository;
import com.tp1.habittracker.repository.UserRepository;
import com.tp1.habittracker.repository.graph.RecommendationCandidate;
import com.tp1.habittracker.repository.graph.UserGraphRepository;
import com.tp1.habittracker.util.SimilarityUtils;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Two-stage recommender:
 *   1. Graph stage (Neo4j) — friends + friends-of-friends habits the user does NOT already own,
 *      with their minimum graph distance.
 *   2. Similarity stage (Mongo embeddings + cosine) — score each candidate against the user's
 *      own habits, apply a distance-based weight, threshold, and sort.
 *
 * Habits without an embedding are skipped. Users with no habits get no recommendations
 * (we need a baseline to compute similarity against).
 */
@Service
@RequiredArgsConstructor
public class HabitRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(HabitRecommendationService.class);

    private final HabitRepository habitRepository;
    private final UserRepository userRepository;
    private final UserGraphRepository userGraphRepository;
    private final HabitRecommendationProperties properties;

    public List<HabitRecommendationResponse> recommendForUser(String authenticatedUserId) {
        String userId = Objects.requireNonNull(authenticatedUserId, "authenticated userId must not be null");
        requireUserExists(userId);

        List<Habit> ownHabits = habitRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(h -> h.getEmbedding() != null && !h.getEmbedding().isEmpty())
                .toList();

        if (ownHabits.isEmpty()) {
            return List.of();
        }

        List<RecommendationCandidate> candidates = userGraphRepository.findRecommendationCandidates(
                userId, properties.getMaxCandidates());

        if (candidates.isEmpty()) {
            return List.of();
        }

        Map<String, Integer> distanceByHabitId = candidates.stream()
                .collect(Collectors.toMap(
                        RecommendationCandidate::habitId,
                        RecommendationCandidate::distance,
                        Math::min
                ));

        List<Habit> candidateHabits = habitRepository.findAllById(distanceByHabitId.keySet());

        Map<String, String> usernameByUserId = resolveOwnerUsernames(candidateHabits);

        List<HabitRecommendationResponse> scored = new java.util.ArrayList<>();
        for (Habit candidate : candidateHabits) {
            if (candidate.isDefault() || candidate.getUserId() == null) {
                continue;
            }
            if (candidate.getEmbedding() == null || candidate.getEmbedding().isEmpty()) {
                continue;
            }

            ScoredMatch match = bestMatch(ownHabits, candidate);
            if (match == null) {
                continue;
            }
            if (match.similarity() < properties.getSimilarityThreshold()) {
                continue;
            }

            int distance = distanceByHabitId.getOrDefault(candidate.getId(), 1);
            double weight = weightForDistance(distance);
            double weightedScore = match.similarity() * weight;

            scored.add(new HabitRecommendationResponse(
                    candidate.getId(),
                    candidate.getName(),
                    candidate.getType(),
                    candidate.getFrequency(),
                    candidate.getUserId(),
                    usernameByUserId.getOrDefault(candidate.getUserId(), null),
                    distance,
                    match.similarity(),
                    weightedScore,
                    new ClosestOwnHabit(match.ownHabit().getId(), match.ownHabit().getName())
            ));
        }

        scored.sort(Comparator.comparingDouble(HabitRecommendationResponse::weightedScore).reversed()
                .thenComparing(HabitRecommendationResponse::habitId));

        if (scored.size() > properties.getMaxResults()) {
            return scored.subList(0, properties.getMaxResults());
        }
        return scored;
    }

    private ScoredMatch bestMatch(List<Habit> ownHabits, Habit candidate) {
        ScoredMatch best = null;
        for (Habit own : ownHabits) {
            try {
                double sim = SimilarityUtils.cosineSimilarity(own.getEmbedding(), candidate.getEmbedding());
                if (best == null || sim > best.similarity()) {
                    best = new ScoredMatch(own, sim);
                }
            } catch (IllegalArgumentException ex) {
                log.debug("Skipping cosine for own={} candidate={}: {}",
                        own.getId(), candidate.getId(), ex.getMessage());
            }
        }
        return best;
    }

    private double weightForDistance(int distance) {
        return switch (distance) {
            case 1 -> properties.getFriendDistanceWeight();
            case 2 -> properties.getFofDistanceWeight();
            default -> properties.getFofDistanceWeight();
        };
    }

    private Map<String, String> resolveOwnerUsernames(List<Habit> candidateHabits) {
        List<UUID> ownerIds = candidateHabits.stream()
                .map(Habit::getUserId)
                .filter(Objects::nonNull)
                .map(this::parseUuidOrNull)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (ownerIds.isEmpty()) {
            return Map.of();
        }

        Map<String, String> byId = new HashMap<>();
        for (User user : userRepository.findAllById(ownerIds)) {
            byId.put(user.getId().toString(), user.getUsername());
        }
        return byId;
    }

    private UUID parseUuidOrNull(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void requireUserExists(String userId) {
        UUID parsed;
        try {
            parsed = UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        if (!userRepository.existsById(parsed)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
    }

    private record ScoredMatch(Habit ownHabit, double similarity) {
    }
}

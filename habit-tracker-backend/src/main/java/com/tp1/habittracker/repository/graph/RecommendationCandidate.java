package com.tp1.habittracker.repository.graph;

/**
 * Projection returned by the friends/friends-of-friends recommendation query.
 * {@code distance} is the shortest path length in the friendship graph (1 = friend,
 * 2 = friend-of-friend) and is used to weight scores downstream.
 */
public record RecommendationCandidate(String habitId, int distance) {
}

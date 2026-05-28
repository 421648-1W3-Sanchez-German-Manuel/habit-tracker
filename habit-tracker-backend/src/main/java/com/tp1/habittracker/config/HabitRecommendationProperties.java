package com.tp1.habittracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.recommendation")
public class HabitRecommendationProperties {

    /** Minimum cosine similarity required for a friend's habit to be recommended. */
    private double similarityThreshold = 0.5;

    /** Maximum candidate habit ids returned by the graph traversal stage. */
    private int maxCandidates = 50;

    /** Maximum number of recommendations returned to the client. */
    private int maxResults = 10;

    /** Multiplier applied to similarity for habits owned by direct friends (graph distance 1). */
    private double friendDistanceWeight = 1.0;

    /** Multiplier applied to similarity for habits owned by friends-of-friends (graph distance 2). */
    private double fofDistanceWeight = 0.7;

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public int getMaxCandidates() {
        return maxCandidates;
    }

    public void setMaxCandidates(int maxCandidates) {
        this.maxCandidates = maxCandidates;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public double getFriendDistanceWeight() {
        return friendDistanceWeight;
    }

    public void setFriendDistanceWeight(double friendDistanceWeight) {
        this.friendDistanceWeight = friendDistanceWeight;
    }

    public double getFofDistanceWeight() {
        return fofDistanceWeight;
    }

    public void setFofDistanceWeight(double fofDistanceWeight) {
        this.fofDistanceWeight = fofDistanceWeight;
    }
}

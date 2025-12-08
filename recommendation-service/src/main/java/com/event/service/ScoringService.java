package com.event.service;

import com.event.dto.EventDTO;
import com.event.dto.InteractionDTO;
import com.event.dto.UserPreferencesDTO;
import com.event.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ScoringService {
    @Value("${recommendation.weights.geo}")
    private double geoWeight;

    @Value("${recommendation.weights.interest}")
    private double interestWeight;

    @Value("${recommendation.weights.interaction}")
    private double interactionWeight;

    @Value("${recommendation.weights.popularity}")
    private double popularityWeight;

    @Value("${recommendation.weights.recency}")
    private double recencyWeight;

    /**
     * Calculate overall recommendation score for an event
     *
     * Score = (geoWeight × geoScore) +
     *         (interestWeight × interestScore) +
     *         (interactionWeight × interactionScore) +
     *         (popularityWeight × popularityScore) +
     *         (recencyWeight × recencyScore)
     *
     * @param event Event to score
     * @param user User preferences
     * @param interactions User's past interactions
     * @param distance Distance from user to event (km)
     * @return Map containing overall score and breakdown
     */
    public Map<String, Double> calculateScore(
            EventDTO event,
            UserPreferencesDTO user,
            List<InteractionDTO> interactions,
            double distance
    ) {
        // Calculate individual scores
        double geoScore = calculateGeoScore(distance);
        double interestScore = calculateInterestScore(user, event);
        double interactionScore = calculateInteractionScore(event, interactions);
        double popularityScore = calculatePopularityScore(event);
        double recencyScore = calculateRecencyScore(event);

        // Calculate weighted final score
        double finalScore = (geoWeight * geoScore) +
                (interestWeight * interestScore) +
                (interactionWeight * interactionScore) +
                (popularityWeight * popularityScore) +
                (recencyWeight * recencyScore);

        // Ensure score is between 0 and 1
        finalScore = Math.max(0.0, Math.min(1.0, finalScore));

        // Return score breakdown
        Map<String, Double> scoreMap = new HashMap<>();
        scoreMap.put("finalScore", finalScore);
        scoreMap.put("geoScore", geoScore);
        scoreMap.put("interestScore", interestScore);
        scoreMap.put("interactionScore", interactionScore);
        scoreMap.put("popularityScore", popularityScore);
        scoreMap.put("recencyScore", recencyScore);

        log.debug("Scored event {}: final={}, geo={}, interest={}, interaction={}, popularity={}, recency={}",
                event.getTitle(), finalScore, geoScore, interestScore, interactionScore, popularityScore, recencyScore);

        return scoreMap;
    }

    /**
     * Calculate cold start score (for users with no interactions)
     * Emphasizes geo and popularity over interest and interaction
     *
     * @param event Event to score
     * @param user User preferences
     * @param distance Distance from user to event
     * @return Map containing overall score and breakdown
     */
    public Map<String, Double> calculateColdStartScore(
            EventDTO event,
            UserPreferencesDTO user,
            double distance
    ) {
        // Adjust weights for cold start
        // Geo: 60%, Popularity: 30%, Recency: 10%
        double coldStartGeoWeight = 0.60;
        double coldStartPopularityWeight = 0.30;
        double coldStartRecencyWeight = 0.10;

        double geoScore = calculateGeoScore(distance);
        double popularityScore = calculatePopularityScore(event);
        double recencyScore = calculateRecencyScore(event);

        // Interest score (if user has set interests)
        double interestScore = 0.0;
        if (user.hasInterests()) {
            interestScore = calculateInterestScore(user, event);
            // Blend in interest score (20% weight)
            coldStartGeoWeight = 0.50;
            coldStartPopularityWeight = 0.20;
            double coldStartInterestWeight = 0.20;

            double finalScore = (coldStartGeoWeight * geoScore) +
                    (coldStartInterestWeight * interestScore) +
                    (coldStartPopularityWeight * popularityScore) +
                    (coldStartRecencyWeight * recencyScore);

            Map<String, Double> scoreMap = new HashMap<>();
            scoreMap.put("finalScore", Math.max(0.0, Math.min(1.0, finalScore)));
            scoreMap.put("geoScore", geoScore);
            scoreMap.put("interestScore", interestScore);
            scoreMap.put("interactionScore", 0.0);
            scoreMap.put("popularityScore", popularityScore);
            scoreMap.put("recencyScore", recencyScore);
            return scoreMap;
        }

        double finalScore = (coldStartGeoWeight * geoScore) +
                (coldStartPopularityWeight * popularityScore) +
                (coldStartRecencyWeight * recencyScore);

        Map<String, Double> scoreMap = new HashMap<>();
        scoreMap.put("finalScore", Math.max(0.0, Math.min(1.0, finalScore)));
        scoreMap.put("geoScore", geoScore);
        scoreMap.put("interestScore", 0.0);
        scoreMap.put("interactionScore", 0.0);
        scoreMap.put("popularityScore", popularityScore);
        scoreMap.put("recencyScore", recencyScore);

        return scoreMap;
    }

    /**
     * Calculate geo score based on distance
     * Closer events get higher scores
     */
    private double calculateGeoScore(double distance) {
        return GeoUtils.calculateGeoScore(distance);
    }

    /**
     * Calculate interest score based on category match
     * 1.0 if event category matches user interests, 0.0 otherwise
     */
    private double calculateInterestScore(UserPreferencesDTO user, EventDTO event) {
        if (user.getInterests() == null || user.getInterests().isEmpty()) {
            return 0.0;
        }

        if (event.getCategory() == null) {
            return 0.0;
        }

        // Check if event category matches any user interest
        String eventCategory = event.getCategoryName().toLowerCase();
        for (String interest : user.getInterests()) {
            if (interest.toLowerCase().equals(eventCategory)) {
                return 1.0;
            }
        }

        // TODO: Future enhancement - category similarity
        // e.g., music and entertainment are similar

        return 0.0;
    }

    /**
     * Calculate interaction score based on past user behavior
     * Higher score for users who interacted with similar events
     */
    private double calculateInteractionScore(EventDTO event, List<InteractionDTO> interactions) {
        if (interactions == null || interactions.isEmpty()) {
            return 0.0;
        }

        // Count interactions with same category
        long sameCategoryCount = interactions.stream()
                .filter(i -> i.getCategory() != null &&
                        i.getCategory().equalsIgnoreCase(event.getCategoryName()))
                .count();

        // Weight by interaction type
        double weightedScore = interactions.stream()
                .filter(i -> i.getCategory() != null &&
                        i.getCategory().equalsIgnoreCase(event.getCategoryName()))
                .mapToDouble(InteractionDTO::getInteractionWeight)
                .sum();

        // Normalize to 0-1 scale (cap at 10 interactions for max score)
        double normalizedScore = Math.min(weightedScore / 10.0, 1.0);

        log.debug("Interaction score for category {}: {} interactions, weighted score: {}, normalized: {}",
                event.getCategory(), sameCategoryCount, weightedScore, normalizedScore);

        return normalizedScore;
    }

    /**
     * Calculate popularity score based on event interactions
     * More popular events get higher scores
     */
    private double calculatePopularityScore(EventDTO event) {
        long totalInteractions = event.getTotalInteractions();

        // Scoring tiers based on total interactions
        if (totalInteractions >= 100) {
            return 1.0;  // Very popular
        } else if (totalInteractions >= 50) {
            return 0.7;  // Popular
        } else if (totalInteractions >= 20) {
            return 0.4;  // Somewhat popular
        } else if (totalInteractions >= 5) {
            return 0.2;  // New/emerging
        } else {
            return 0.1;  // Very new
        }
    }

    /**
     * Calculate recency score based on how soon event is happening
     * Events happening soon get higher scores
     */
    private double calculateRecencyScore(EventDTO event) {
        if (event.getStartTime() == null) {
            return 0.0;
        }

        LocalDateTime now = LocalDateTime.now();
        long daysUntilEvent = ChronoUnit.DAYS.between(now, event.getStartTime());

        // Events in the past get 0 score
        if (daysUntilEvent < 0) {
            return 0.0;
        }

        // Scoring tiers
        if (daysUntilEvent <= 3) {
            return 1.0;  // This weekend!
        } else if (daysUntilEvent <= 7) {
            return 0.8;  // This week
        } else if (daysUntilEvent <= 14) {
            return 0.5;  // Next two weeks
        } else if (daysUntilEvent <= 30) {
            return 0.3;  // This month
        } else {
            return 0.1;  // Far future
        }
    }

    /**
     * Generate human-readable reasons for recommendation
     */
    public List<String> generateReasons(
            EventDTO event,
            UserPreferencesDTO user,
            Map<String, Double> scores,
            double distance
    ) {
        List<String> reasons = new java.util.ArrayList<>();

        // Distance reason
        if (scores.get("geoScore") >= 0.8) {
            reasons.add("Only " + String.format("%.1f", distance) + " km away");
        } else if (scores.get("geoScore") >= 0.5) {
            reasons.add("Within your area (" + String.format("%.1f", distance) + " km)");
        }

        // Interest match reason
        if (scores.get("interestScore") == 1.0) {
            reasons.add("Matches your interest in " + event.getCategory());
        }

        // Interaction reason
        if (scores.get("interactionScore") >= 0.5) {
            reasons.add("Similar to events you've saved");
        }

        // Popularity reason
        if (scores.get("popularityScore") >= 0.7) {
            reasons.add("Trending in your area");
        }

        // Recency reason
        if (scores.get("recencyScore") == 1.0) {
            reasons.add("Happening this weekend!");
        } else if (scores.get("recencyScore") >= 0.8) {
            reasons.add("Coming up this week");
        }

        // Free event reason
        if (event.isFree()) {
            reasons.add("Free event");
        }

        // Verified reason
        if (Boolean.TRUE.equals(event.getVerified())) {
            reasons.add("Verified host");
        }

        return reasons;
    }
}

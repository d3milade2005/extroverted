package com.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.event.dto.*;
import com.event.entity.RecommendationHistory;
import com.event.repository.RecommendationHistoryRepository;
import com.event.util.GeoUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@RequiredArgsConstructor
@Service
public class RecommendationService {
    private final UserServiceClient userServiceClient;
    private final EventServiceClient eventServiceClient;
    private final ScoringService scoringService;
    private final CacheService cacheService;
    private final RecommendationHistoryRepository historyRepository;

    @Value("${recommendation.pagination.default-size}")
    private int defaultPageSize;

    @Value("${recommendation.pagination.max-size}")
    private int maxPageSize;

    @Value("${recommendation.cold-start.default-radius-km}")
    private double defaultRadiusKm;

    /**
     * Get personalized recommendations for a user
     *
     * @param userId User ID
     * @param request Recommendation request parameters
     * @param token JWT token for service calls
     * @return List of recommended events
     */
    public List<EventRecommendationResponse> getPersonalizedRecommendations(
            UUID userId,
            RecommendationRequest request,
            String token
    ) {
        log.info("Getting recommendations for user: {}", userId);

        // Check cache first (unless refresh requested)
        if (!Boolean.TRUE.equals(request.getRefresh())) {
            List<EventRecommendationResponse> cached =
                    cacheService.getUserRecommendations(userId, request.getPage());
            if (cached != null && !cached.isEmpty()) {
                log.info("Returning cached recommendations for user {}", userId);
                return cached;
            }
        }

        // Fetch user preferences
        UserPreferencesDTO user = userServiceClient.getUserPreferences(userId, token);
        if (user == null) {
            log.warn("Could not fetch user preferences for user {}", userId);
            return Collections.emptyList();
        }

        // Fetch user interactions
        List<InteractionDTO> interactions = eventServiceClient.getUserInteractions(userId, token);
        user.setHasInteractions(interactions != null && !interactions.isEmpty());

        // Determine if cold start
        boolean isColdStart = user.isColdStart();
        log.info("User {} cold start: {}", userId, isColdStart);

        // Fetch events
        List<EventDTO> events = fetchRelevantEvents(user, request);
        if (events.isEmpty()) {
            log.info("No events found for user {}", userId);
            return Collections.emptyList();
        }

        // Score and rank events
        List<EventRecommendationResponse> recommendations;
        if (isColdStart) {
            recommendations = scoreEventsColdStart(events, user, request);
        } else {
            recommendations = scoreEvents(events, user, interactions, request);
        }

        // Apply pagination
        recommendations = applyPagination(recommendations, request);

        // Cache results
        cacheService.cacheUserRecommendations(userId, request.getPage(), recommendations);

        // Async: Save to history
        saveToHistoryAsync(userId, recommendations);

        log.info("Returning {} recommendations for user {}", recommendations.size(), userId);
        return recommendations;
    }

    /**
     * Get trending events (public endpoint)
     *
     * @param limit Maximum number of events
     * @return List of trending events
     */
    public List<EventRecommendationResponse> getTrendingEvents(int limit) {
        log.info("Getting trending events");

        // Check cache
        List<EventRecommendationResponse> cached = cacheService.getTrendingEvents();
        if (cached != null && !cached.isEmpty()) {
            return cached.stream().limit(limit).collect(Collectors.toList());
        }

        // Fetch upcoming events
        List<EventDTO> events = eventServiceClient.getUpcomingEvents(100);
        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        // Sort by popularity and recency
        List<EventRecommendationResponse> trending = events.stream()
                .map(event -> {
                    // Simple scoring for trending (popularity + recency only)
                    double popularityScore = calculatePopularityOnly(event);
                    double recencyScore = calculateRecencyOnly(event);
                    double score = (0.7 * popularityScore) + (0.3 * recencyScore);

                    return mapToResponse(event, score, 0.0, Collections.singletonList("Trending now"));
                })
                .sorted(Comparator.comparingDouble(EventRecommendationResponse::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        // Cache results
        cacheService.cacheTrendingEvents(trending);

        return trending;
    }

    /**
     * Get similar events based on an event
     *
     * @param eventId Event ID to find similar events for
     * @param limit Maximum number of similar events
     * @return List of similar events
     */
    public List<EventRecommendationResponse> getSimilarEvents(UUID eventId, int limit) {
        log.info("Getting similar events for event: {}", eventId);

        // Check cache
        List<EventRecommendationResponse> cached = cacheService.getSimilarEvents(eventId);
        if (cached != null && !cached.isEmpty()) {
            return cached.stream().limit(limit).collect(Collectors.toList());
        }

        // Fetch all upcoming events
        List<EventDTO> events = eventServiceClient.getUpcomingEvents(100);

        // Find the target event
        Optional<EventDTO> targetEvent = events.stream()
                .filter(e -> e.getId().equals(eventId))
                .findFirst();

        if (targetEvent.isEmpty()) {
            log.warn("Target event not found: {}", eventId);
            return Collections.emptyList();
        }

        EventDTO target = targetEvent.get();

        // Find similar events (same category, nearby)
        List<EventRecommendationResponse> similar = events.stream()
                .filter(e -> !e.getId().equals(eventId))  // Exclude the target event itself
                .filter(e -> e.getCategory() != null &&
                        e.getCategory().equalsIgnoreCase(target.getCategory()))  // Same category
                .map(event -> {
                    double distance = 0.0;
                    if (target.getLocation() != null && event.getLocation() != null) {
                        distance = GeoUtils.calculateDistance(target.getLocation(), event.getLocation());
                    }

                    // Similarity score based on distance and popularity
                    double geoScore = GeoUtils.calculateGeoScore(distance);
                    double popularityScore = calculatePopularityOnly(event);
                    double score = (0.6 * geoScore) + (0.4 * popularityScore);

                    return mapToResponse(event, score, distance,
                            Collections.singletonList("Similar to this event"));
                })
                .sorted(Comparator.comparingDouble(EventRecommendationResponse::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        // Cache results
        cacheService.cacheSimilarEvents(eventId, similar);

        return similar;
    }

    /**
     * Get recommendations by category
     *
     * @param userId User ID
     * @param category Category name
     * @param limit Maximum number of events
     * @param token JWT token
     * @return List of events in category, scored for user
     */
    public List<EventRecommendationResponse> getRecommendationsByCategory(
            UUID userId,
            String category,
            int limit,
            String token
    ) {
        log.info("Getting {} recommendations for user {} in category: {}", limit, userId, category);

        // Check cache
        List<EventRecommendationResponse> cached =
                cacheService.getCategoryRecommendations(userId, category);
        if (cached != null && !cached.isEmpty()) {
            return cached.stream().limit(limit).collect(Collectors.toList());
        }

        // Fetch user preferences and interactions
        UserPreferencesDTO user = userServiceClient.getUserPreferences(userId, token);
        List<InteractionDTO> interactions = eventServiceClient.getUserInteractions(userId, token);

        // Fetch upcoming events (filter by category on our side)
        List<EventDTO> events = eventServiceClient.getUpcomingEvents(100);
        events = events.stream()
                .filter(e -> category.equalsIgnoreCase(e.getCategory()))
                .collect(Collectors.toList());

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        // Score events
        List<EventRecommendationResponse> recommendations;
        if (user.isColdStart()) {
            recommendations = scoreEventsColdStart(events, user, new RecommendationRequest());
        } else {
            recommendations = scoreEvents(events, user, interactions, new RecommendationRequest());
        }

        recommendations = recommendations.stream()
                .limit(limit)
                .collect(Collectors.toList());

        // Cache results
        cacheService.cacheCategoryRecommendations(userId, category, recommendations);

        return recommendations;
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Fetch relevant events based on user location and filters
     */
    private List<EventDTO> fetchRelevantEvents(UserPreferencesDTO user, RecommendationRequest request) {
        List<EventDTO> events;

        if (user.hasLocation()) {
            // Fetch events near user
            double radius = request.getMaxDistanceKm() != null ?
                    request.getMaxDistanceKm() : defaultRadiusKm;
            events = eventServiceClient.getNearbyEvents(
                    user.getLocation().getLatitude(),
                    user.getLocation().getLongitude(),
                    radius
            );
        } else {
            // Fetch general upcoming events
            events = eventServiceClient.getUpcomingEvents(100);
        }

        // Apply filters
        return applyFilters(events, request);
    }

    /**
     * Apply filters to event list
     */
    private List<EventDTO> applyFilters(List<EventDTO> events, RecommendationRequest request) {
        return events.stream()
                .filter(event -> {
                    // Category filter
                    if (request.getCategoryFilter() != null &&
                            !request.getCategoryFilter().equalsIgnoreCase(event.getCategory())) {
                        return false;
                    }

                    // Price filter
                    if (request.getMaxPrice() != null && event.getTicketPrice() != null &&
                            event.getTicketPrice().compareTo(request.getMaxPrice()) > 0) {
                        return false;
                    }

                    // Free only filter
                    if (Boolean.TRUE.equals(request.getFreeOnly()) && !event.isFree()) {
                        return false;
                    }

                    // Verified only filter
                    if (Boolean.TRUE.equals(request.getVerifiedOnly()) &&
                            !Boolean.TRUE.equals(event.getVerified())) {
                        return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Score events for regular users (with interaction history)
     */
    private List<EventRecommendationResponse> scoreEvents(
            List<EventDTO> events,
            UserPreferencesDTO user,
            List<InteractionDTO> interactions,
            RecommendationRequest request
    ) {
        return events.stream()
                .map(event -> {
                    // Calculate distance
                    double distance = 0.0;
                    if (user.hasLocation() && event.getLocation() != null) {
                        distance = GeoUtils.calculateDistance(user.getLocation(), event.getLocation());
                    }

                    // Filter by max distance if specified
                    if (request.getMaxDistanceKm() != null && distance > request.getMaxDistanceKm()) {
                        return null;
                    }

                    // Calculate score
                    Map<String, Double> scores = scoringService.calculateScore(
                            event, user, interactions, distance
                    );

                    // Generate reasons
                    List<String> reasons = scoringService.generateReasons(event, user, scores, distance);

                    // Map to response
                    return mapToResponse(event, scores, distance, reasons);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(EventRecommendationResponse::getScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Score events for cold start users (no interaction history)
     */
    private List<EventRecommendationResponse> scoreEventsColdStart(
            List<EventDTO> events,
            UserPreferencesDTO user,
            RecommendationRequest request
    ) {
        return events.stream()
                .map(event -> {
                    // Calculate distance
                    double distance = 0.0;
                    if (user.hasLocation() && event.getLocation() != null) {
                        distance = GeoUtils.calculateDistance(user.getLocation(), event.getLocation());
                    }

                    // Filter by max distance if specified
                    if (request.getMaxDistanceKm() != null && distance > request.getMaxDistanceKm()) {
                        return null;
                    }

                    // Calculate cold start score
                    Map<String, Double> scores = scoringService.calculateColdStartScore(
                            event, user, distance
                    );

                    // Generate reasons (cold start version)
                    List<String> reasons = new ArrayList<>();
                    reasons.add("Popular in your area");
                    if (distance < 10) {
                        reasons.add(String.format("Only %.1f km away", distance));
                    }
                    if (event.isFree()) {
                        reasons.add("Free event");
                    }

                    // Map to response
                    return mapToResponse(event, scores, distance, reasons);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(EventRecommendationResponse::getScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Apply pagination to recommendations
     */
    private List<EventRecommendationResponse> applyPagination(
            List<EventRecommendationResponse> recommendations,
            RecommendationRequest request
    ) {
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : defaultPageSize;
        size = Math.min(size, maxPageSize);

        int fromIndex = page * size;
        if (fromIndex >= recommendations.size()) {
            return Collections.emptyList();
        }

        int toIndex = Math.min(fromIndex + size, recommendations.size());
        return recommendations.subList(fromIndex, toIndex);
    }

    /**
     * Map EventDTO to EventRecommendationResponse
     */
    private EventRecommendationResponse mapToResponse(
            EventDTO event,
            Map<String, Double> scores,
            double distance,
            List<String> reasons
    ) {
        return EventRecommendationResponse.builder()
                .eventId(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .category(event.getCategory())
                .venue(event.getVenue())
                .address(event.getAddress())
                .location(event.getLocation())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .ticketPrice(event.getTicketPrice())
                .ticketLimit(event.getTicketLimit())
                .ticketsSold(event.getTicketsSold())
                .hasAvailableTickets(event.hasAvailableTickets())
                .imageUrl(event.getImageUrl())
                .verified(event.getVerified())
                .score(scores.get("finalScore"))
                .distanceKm(distance > 0 ? distance : null)
                .reasons(reasons)
                .scoreBreakdown(EventRecommendationResponse.ScoreBreakdown.builder()
                        .geoScore(scores.get("geoScore"))
                        .interestScore(scores.get("interestScore"))
                        .interactionScore(scores.get("interactionScore"))
                        .popularityScore(scores.get("popularityScore"))
                        .recencyScore(scores.get("recencyScore"))
                        .build())
                .build();
    }

    /**
     * Map EventDTO to EventRecommendationResponse (simple version)
     */
    private EventRecommendationResponse mapToResponse(
            EventDTO event,
            double score,
            double distance,
            List<String> reasons
    ) {
        return EventRecommendationResponse.builder()
                .eventId(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .category(event.getCategory())
                .venue(event.getVenue())
                .address(event.getAddress())
                .location(event.getLocation())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .ticketPrice(event.getTicketPrice())
                .ticketLimit(event.getTicketLimit())
                .ticketsSold(event.getTicketsSold())
                .hasAvailableTickets(event.hasAvailableTickets())
                .imageUrl(event.getImageUrl())
                .verified(event.getVerified())
                .score(score)
                .distanceKm(distance > 0 ? distance : null)
                .reasons(reasons)
                .build();
    }

    /**
     * Save recommendations to history (async)
     */
    @Async
    protected void saveToHistoryAsync(UUID userId, List<EventRecommendationResponse> recommendations) {
        try {
            int rank = 1;
            for (EventRecommendationResponse rec : recommendations) {
                RecommendationHistory history = RecommendationHistory.builder()
                        .userId(userId)
                        .eventId(rec.getEventId())
                        .score(rec.getScore())
                        .rankPosition(rank++)
                        .geoScore(rec.getScoreBreakdown() != null ?
                                rec.getScoreBreakdown().getGeoScore() : null)
                        .interestScore(rec.getScoreBreakdown() != null ?
                                rec.getScoreBreakdown().getInterestScore() : null)
                        .interactionScore(rec.getScoreBreakdown() != null ?
                                rec.getScoreBreakdown().getInteractionScore() : null)
                        .popularityScore(rec.getScoreBreakdown() != null ?
                                rec.getScoreBreakdown().getPopularityScore() : null)
                        .recencyScore(rec.getScoreBreakdown() != null ?
                                rec.getScoreBreakdown().getRecencyScore() : null)
                        .recommendedAt(LocalDateTime.now())
                        .recommendationReason(rec.getReasons() != null ?
                                rec.getReasons().toArray(new String[0]) : null)
                        .distanceKm(rec.getDistanceKm())
                        .algorithmVersion("v1.0")
                        .build();

                historyRepository.save(history);
            }
            log.debug("Saved {} recommendations to history for user {}",
                    recommendations.size(), userId);
        } catch (Exception e) {
            log.error("Error saving recommendations to history: {}", e.getMessage());
        }
    }

    /**
     * Calculate popularity score only (for trending)
     */
    private double calculatePopularityOnly(EventDTO event) {
        long totalInteractions = event.getTotalInteractions();
        if (totalInteractions >= 100) return 1.0;
        if (totalInteractions >= 50) return 0.7;
        if (totalInteractions >= 20) return 0.4;
        if (totalInteractions >= 5) return 0.2;
        return 0.1;
    }

    /**
     * Calculate recency score only (for trending)
     */
    private double calculateRecencyOnly(EventDTO event) {
        if (event.getStartTime() == null) return 0.0;

        LocalDateTime now = LocalDateTime.now();
        long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(now, event.getStartTime());

        if (daysUntil < 0) return 0.0;
        if (daysUntil <= 3) return 1.0;
        if (daysUntil <= 7) return 0.8;
        if (daysUntil <= 14) return 0.5;
        if (daysUntil <= 30) return 0.3;
        return 0.1;
    }
}

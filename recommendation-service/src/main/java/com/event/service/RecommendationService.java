package com.event.service;

import com.event.dto.*;
import com.event.entity.RecommendationHistory;
import com.event.repository.RecommendationHistoryRepository;
import com.event.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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

    // ✅ FIX 1: Self-injection allows calling @Async methods within the same class
    @Autowired
    @Lazy // Prevents circular dependency errors on startup
    private RecommendationService self;

    // ✅ FIX 2: Added default values (:20, :100) to prevent startup crashes if config is missing
    @Value("${recommendation.pagination.default-size:20}")
    private int defaultPageSize;

    @Value("${recommendation.pagination.max-size:100}")
    private int maxPageSize;

    @Value("${recommendation.cold-start.default-radius-km:20.0}")
    private double defaultRadiusKm;

    public List<EventRecommendationResponse> getPersonalizedRecommendations(
            UUID userId,
            RecommendationRequest request,
            String token
    ) {
        // 1. Check Cache
        if (!Boolean.TRUE.equals(request.getRefresh())) {
            List<EventRecommendationResponse> cached =
                    cacheService.getUserRecommendations(userId, request.getPage());
            if (cached != null && !cached.isEmpty()) {
                log.info("Returning cached recommendations for user {}", userId);
                return cached;
            }
        }

        // 2. Fetch User Preferences
        UserPreferencesDTO user = userServiceClient.getUserPreferences(userId, token);
        // Note: Client now handles errors and returns defaults, but we check for null safety
        if (user == null) {
            return Collections.emptyList();
        }

        // 3. Fetch Interactions
        List<InteractionDTO> interactions = eventServiceClient.getUserInteractions(userId, token);
        user.setHasInteractions(interactions != null && !interactions.isEmpty());

        log.info("User {} cold start: {}", userId, user.isColdStart());

        // 4. Fetch Candidate Events
        List<EventDTO> events = fetchRelevantEvents(user, request);
        if (events.isEmpty()) {
            log.info("No events found for user {}", userId);
            return Collections.emptyList();
        }

        // 5. Score Events
        List<EventRecommendationResponse> recommendations;
        if (user.isColdStart()) {
            recommendations = scoreEventsColdStart(events, user, request);
        } else {
            recommendations = scoreEvents(events, user, interactions, request);
        }

        // 6. Pagination
        recommendations = applyPagination(recommendations, request);

        // 7. Caching
        cacheService.cacheUserRecommendations(userId, request.getPage(), recommendations);

        // 8. Async History Saving
        // ✅ FIX: Call via 'self' to ensure the @Async proxy triggers
        self.saveToHistoryAsync(userId, recommendations);

        log.info("Returning {} recommendations for user {}", recommendations.size(), userId);
        return recommendations;
    }

    public List<EventRecommendationResponse> getTrendingEvents(int limit) {
        log.info("Getting trending events");

        // Check cache
        List<EventRecommendationResponse> cached = cacheService.getTrendingEvents();
        if (cached != null && !cached.isEmpty()) {
            return cached.stream().limit(limit).collect(Collectors.toList());
        }

        // Fetch upcoming events (fetch more to ensure we have enough after scoring)
        List<EventDTO> events = eventServiceClient.getUpcomingEvents(100);
        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        List<EventRecommendationResponse> trending = events.stream()
                .map(event -> {
                    double popularityScore = calculatePopularityOnly(event);
                    double recencyScore = calculateRecencyOnly(event);
                    double score = (0.7 * popularityScore) + (0.3 * recencyScore);

                    return mapToResponse(event, score, 0.0, Collections.singletonList("Trending now"));
                })
                .sorted(Comparator.comparingDouble(EventRecommendationResponse::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        cacheService.cacheTrendingEvents(trending);
        return trending;
    }

    public List<EventRecommendationResponse> getSimilarEvents(UUID eventId, int limit) {
        log.info("Getting similar events for event: {}", eventId);

        List<EventRecommendationResponse> cached = cacheService.getSimilarEvents(eventId);
        if (cached != null && !cached.isEmpty()) {
            return cached.stream().limit(limit).collect(Collectors.toList());
        }

        List<EventDTO> events = eventServiceClient.getUpcomingEvents(100);

        Optional<EventDTO> targetEvent = events.stream()
                .filter(e -> e.getId().equals(eventId))
                .findFirst();

        if (targetEvent.isEmpty()) {
            log.warn("Target event not found: {}", eventId);
            return Collections.emptyList();
        }

        EventDTO target = targetEvent.get();

        List<EventRecommendationResponse> similar = events.stream()
                .filter(e -> !e.getId().equals(eventId))
                // ✅ FIX: Use getCategoryName() to avoid Object vs String crash
                .filter(e -> e.getCategoryName() != null &&
                        e.getCategoryName().equalsIgnoreCase(target.getCategoryName()))
                .map(event -> {
                    double distance = 0.0;
                    if (target.getLocation() != null && event.getLocation() != null) {
                        distance = GeoUtils.calculateDistance(target.getLocation(), event.getLocation());
                    }

                    double geoScore = GeoUtils.calculateGeoScore(distance);
                    double popularityScore = calculatePopularityOnly(event);
                    double score = (0.6 * geoScore) + (0.4 * popularityScore);

                    return mapToResponse(event, score, distance, Collections.singletonList("Similar to this event"));
                })
                .sorted(Comparator.comparingDouble(EventRecommendationResponse::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        cacheService.cacheSimilarEvents(eventId, similar);
        return similar;
    }

    public List<EventRecommendationResponse> getRecommendationsByCategory(
            UUID userId, String category, int limit, String token) {

        log.info("Getting {} recommendations for user {} in category: {}", limit, userId, category);

        List<EventRecommendationResponse> cached = cacheService.getCategoryRecommendations(userId, category);
        if (cached != null && !cached.isEmpty()) {
            return cached.stream().limit(limit).collect(Collectors.toList());
        }

        UserPreferencesDTO user = userServiceClient.getUserPreferences(userId, token);
        List<InteractionDTO> interactions = eventServiceClient.getUserInteractions(userId, token);

        List<EventDTO> events = eventServiceClient.getUpcomingEvents(100);

        // ✅ FIX: Use getCategoryName() here too
        events = events.stream()
                .filter(e -> category.equalsIgnoreCase(e.getCategoryName()))
                .collect(Collectors.toList());

        if (events.isEmpty()) return Collections.emptyList();

        List<EventRecommendationResponse> recommendations;
        if (user.isColdStart()) {
            recommendations = scoreEventsColdStart(events, user, new RecommendationRequest());
        } else {
            recommendations = scoreEvents(events, user, interactions, new RecommendationRequest());
        }

        recommendations = recommendations.stream().limit(limit).collect(Collectors.toList());
        cacheService.cacheCategoryRecommendations(userId, category, recommendations);

        return recommendations;
    }

    private List<EventDTO> fetchRelevantEvents(UserPreferencesDTO user, RecommendationRequest request) {
        List<EventDTO> events;

        if (user.hasLocation()) {
            double radius = request.getMaxDistanceKm() != null ?
                    request.getMaxDistanceKm() : defaultRadiusKm;
            events = eventServiceClient.getNearbyEvents(
                    user.getLocation().getLatitude(),
                    user.getLocation().getLongitude(),
                    radius
            );
        } else {
            events = eventServiceClient.getUpcomingEvents(100);
        }

        return applyFilters(events, request);
    }

    private List<EventDTO> applyFilters(List<EventDTO> events, RecommendationRequest request) {
        return events.stream()
                .filter(event -> {
                    // ✅ FIX: Use getCategoryName() to prevent crashes
                    if (request.getCategoryFilter() != null &&
                            !request.getCategoryFilter().equalsIgnoreCase(event.getCategoryName())) {
                        return false;
                    }

                    if (request.getMaxPrice() != null && event.getTicketPrice() != null &&
                            event.getTicketPrice().compareTo(request.getMaxPrice()) > 0) {
                        return false;
                    }

                    if (Boolean.TRUE.equals(request.getFreeOnly()) && !event.isFree()) {
                        return false;
                    }

                    if (Boolean.TRUE.equals(request.getVerifiedOnly()) &&
                            !Boolean.TRUE.equals(event.getVerified())) {
                        return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    private List<EventRecommendationResponse> scoreEvents(
            List<EventDTO> events, UserPreferencesDTO user, List<InteractionDTO> interactions, RecommendationRequest request) {
        return events.stream()
                .map(event -> {
                    double distance = 0.0;
                    if (user.hasLocation() && event.getLocation() != null) {
                        distance = GeoUtils.calculateDistance(user.getLocation(), event.getLocation());
                    }

                    if (request.getMaxDistanceKm() != null && distance > request.getMaxDistanceKm()) {
                        return null;
                    }

                    Map<String, Double> scores = scoringService.calculateScore(event, user, interactions, distance);
                    List<String> reasons = scoringService.generateReasons(event, user, scores, distance);

                    return mapToResponse(event, scores, distance, reasons);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(EventRecommendationResponse::getScore).reversed())
                .collect(Collectors.toList());
    }

    private List<EventRecommendationResponse> scoreEventsColdStart(
            List<EventDTO> events, UserPreferencesDTO user, RecommendationRequest request) {
        return events.stream()
                .map(event -> {
                    double distance = 0.0;
                    if (user.hasLocation() && event.getLocation() != null) {
                        distance = GeoUtils.calculateDistance(user.getLocation(), event.getLocation());
                    }

                    if (request.getMaxDistanceKm() != null && distance > request.getMaxDistanceKm()) {
                        return null;
                    }

                    Map<String, Double> scores = scoringService.calculateColdStartScore(event, user, distance);

                    List<String> reasons = new ArrayList<>();
                    reasons.add("Popular in your area");
                    if (distance < 10) reasons.add(String.format("Only %.1f km away", distance));
                    if (event.isFree()) reasons.add("Free event");

                    return mapToResponse(event, scores, distance, reasons);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(EventRecommendationResponse::getScore).reversed())
                .collect(Collectors.toList());
    }

    private List<EventRecommendationResponse> applyPagination(
            List<EventRecommendationResponse> recommendations, RecommendationRequest request) {
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

    private EventRecommendationResponse mapToResponse(
            EventDTO event, Map<String, Double> scores, double distance, List<String> reasons) {
        return EventRecommendationResponse.builder()
                .eventId(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .category(event.getCategoryName()) // ✅ FIX: Use helper
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

    private EventRecommendationResponse mapToResponse(
            EventDTO event, double score, double distance, List<String> reasons) {
        return EventRecommendationResponse.builder()
                .eventId(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .category(event.getCategoryName()) // ✅ FIX: Use helper
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

    // ✅ FIX 3: Async method MUST be public to be proxied
    @Async
    public void saveToHistoryAsync(UUID userId, List<EventRecommendationResponse> recommendations) {
        try {
            int rank = 1;
            List<RecommendationHistory> batchList = new ArrayList<>(); // Use list for batch save

            for (EventRecommendationResponse rec : recommendations) {
                // Safe null checks for breakdown
                Double geo = rec.getScoreBreakdown() != null ? rec.getScoreBreakdown().getGeoScore() : null;
                Double interest = rec.getScoreBreakdown() != null ? rec.getScoreBreakdown().getInterestScore() : null;
                Double interaction = rec.getScoreBreakdown() != null ? rec.getScoreBreakdown().getInteractionScore() : null;
                Double pop = rec.getScoreBreakdown() != null ? rec.getScoreBreakdown().getPopularityScore() : null;
                Double recency = rec.getScoreBreakdown() != null ? rec.getScoreBreakdown().getRecencyScore() : null;

                RecommendationHistory history = RecommendationHistory.builder()
                        .userId(userId)
                        .eventId(rec.getEventId())
                        .score(rec.getScore())
                        .rankPosition(rank++)
                        .geoScore(geo)
                        .interestScore(interest)
                        .interactionScore(interaction)
                        .popularityScore(pop)
                        .recencyScore(recency)
                        .recommendedAt(LocalDateTime.now())
                        .recommendationReason(rec.getReasons() != null ?
                                List.of(rec.getReasons().toArray(new String[0])) : null)
                        .distanceKm(rec.getDistanceKm())
                        .algorithmVersion("v1.0")
                        .build();

                batchList.add(history);
            }
            // Optimization: Save all at once instead of loop
            historyRepository.saveAll(batchList);
            log.debug("Saved {} recommendations to history for user {}", recommendations.size(), userId);
        } catch (Exception e) {
            log.error("Error saving recommendations to history: {}", e.getMessage());
        }
    }

    private double calculatePopularityOnly(EventDTO event) {
        long totalInteractions = event.getTotalInteractions();
        if (totalInteractions >= 100) return 1.0;
        if (totalInteractions >= 50) return 0.7;
        if (totalInteractions >= 20) return 0.4;
        if (totalInteractions >= 5) return 0.2;
        return 0.1;
    }

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
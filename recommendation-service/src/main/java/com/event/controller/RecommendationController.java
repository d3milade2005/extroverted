package com.event.controller;

import com.event.dto.EventRecommendationResponse;
import com.event.dto.RecommendationRequest;
import com.event.service.RecommendationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_USER')")
public class RecommendationController {
    private final RecommendationService recommendationService;

    /**
     * Get personalized recommendations for the authenticated user
     *
     * GET /api/recommendations/events?page=0&size=20&categoryFilter=music&maxDistanceKm=10
     *
     * @param page Page number (default: 0)
     * @param size Page size (default: 20, max: 100)
     * @param categoryFilter Filter by category (optional)
     * @param maxDistanceKm Maximum distance in km (optional)
     * @param maxPrice Maximum ticket price (optional)
     * @param freeOnly Only free events (optional)
     * @param verifiedOnly Only verified events (optional)
     * @param refresh Force refresh (bypass cache) (optional)
     * @param authentication User authentication
     * @param authHeader Authorization header
     * @return List of personalized event recommendations
     */
    @GetMapping("/events")
    public ResponseEntity<List<EventRecommendationResponse>> getPersonalizedRecommendations(
            @RequestParam(required = false, defaultValue = "0") @Min(0) Integer page,
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) String categoryFilter,
            @RequestParam(required = false) Double maxDistanceKm,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(required = false) Boolean freeOnly,
            @RequestParam(required = false) Boolean verifiedOnly,
            @RequestParam(required = false, defaultValue = "false") Boolean refresh,
            Authentication authentication,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        UUID userId = extractUserId(authentication);
        String token = extractToken(authHeader);

        log.info("GET /api/recommendations/events - User: {}, Page: {}, Size: {}",
                userId, page, size);

        RecommendationRequest request = RecommendationRequest.builder()
                .page(page)
                .size(size)
                .categoryFilter(categoryFilter)
                .maxDistanceKm(maxDistanceKm)
                .maxPrice(maxPrice)
                .freeOnly(freeOnly)
                .verifiedOnly(verifiedOnly)
                .refresh(refresh)
                .build();

        List<EventRecommendationResponse> recommendations =
                recommendationService.getPersonalizedRecommendations(userId, request, token);

        return ResponseEntity.ok(recommendations);
    }

    /**
     * Get trending events (public endpoint)
     *
     * GET /api/recommendations/trending?limit=20
     *
     * @param limit Maximum number of events (default: 20, max: 50)
     * @return List of trending events
     */
    @GetMapping("/trending")
    public ResponseEntity<List<EventRecommendationResponse>> getTrendingEvents(
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(50) Integer limit
    ) {
        log.info("GET /api/recommendations/trending - Limit: {}", limit);

        List<EventRecommendationResponse> trending =
                recommendationService.getTrendingEvents(limit);

        return ResponseEntity.ok(trending);
    }

    /**
     * Get events similar to a specific event
     *
     * GET /api/recommendations/similar/{eventId}?limit=10
     *
     * @param eventId Event ID to find similar events for
     * @param limit Maximum number of similar events (default: 10, max: 20)
     * @return List of similar events
     */
    @GetMapping("/similar/{eventId}")
    public ResponseEntity<List<EventRecommendationResponse>> getSimilarEvents(
            @PathVariable UUID eventId,
            @RequestParam(required = false, defaultValue = "10") @Min(1) @Max(20) Integer limit
    ) {
        log.info("GET /api/recommendations/similar/{} - Limit: {}", eventId, limit);

        List<EventRecommendationResponse> similar =
                recommendationService.getSimilarEvents(eventId, limit);

        return ResponseEntity.ok(similar);
    }

    /**
     * Get recommendations by category for authenticated user
     *
     * GET /api/recommendations/category/{category}?limit=20
     *
     * @param category Category name (e.g., "music", "tech", "fashion")
     * @param limit Maximum number of events (default: 20, max: 50)
     * @param authentication User authentication
     * @param authHeader Authorization header
     * @return List of events in category, scored for user
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<EventRecommendationResponse>> getRecommendationsByCategory(
            @PathVariable String category,
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(50) Integer limit,
            Authentication authentication,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        UUID userId = extractUserId(authentication);
        String token = extractToken(authHeader);

        log.info("GET /api/recommendations/category/{} - User: {}, Limit: {}",
                category, userId, limit);

        List<EventRecommendationResponse> recommendations =
                recommendationService.getRecommendationsByCategory(userId, category, limit, token);

        return ResponseEntity.ok(recommendations);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Recommendation Service is healthy");
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Extract user ID from JWT token
     */
    private UUID extractUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String sub = jwt.getSubject();
        return UUID.fromString(sub);
    }

    /**
     * Extract bearer token from Authorization header
     */
    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}

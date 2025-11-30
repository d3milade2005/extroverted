package com.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRecommendationResponse {
    private UUID eventId;
    private String title;
    private String description;
    private String category;

    // Venue information
    private String venue;
    private String address;
    private Location location;

    // Time information
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Ticket information
    private BigDecimal ticketPrice;
    private Integer ticketLimit;
    private Integer ticketsSold;
    private Boolean hasAvailableTickets;

    // Visual
    private String imageUrl;
    private Boolean verified;

    // Recommendation specific
    private Double score;                    // Overall recommendation score (0.0 - 1.0)
    private Double distanceKm;               // Distance from user in kilometers
    private List<String> reasons;            // Why we recommended this event

    // Score breakdown (optional, for debugging)
    private ScoreBreakdown scoreBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreBreakdown {
        private Double geoScore;
        private Double interestScore;
        private Double interactionScore;
        private Double popularityScore;
        private Double recencyScore;
    }
}
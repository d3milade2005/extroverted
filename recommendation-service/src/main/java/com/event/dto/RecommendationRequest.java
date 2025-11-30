package com.event.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationRequest {
    @Min(0)
    @Builder.Default
    private Integer page = 0;

    @Min(1)
    @Max(100)
    @Builder.Default
    private Integer size = 20;

    // Filters
    private String categoryFilter;      // Filter by specific category
    private Double maxDistanceKm;       // Maximum distance in kilometers
    private BigDecimal maxPrice;        // Maximum ticket price
    private Boolean freeOnly;           // Only free events
    private Boolean verifiedOnly;       // Only verified events

    // Force refresh (bypass cache)
    @Builder.Default
    private Boolean refresh = false;
}

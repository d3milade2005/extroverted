package com.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchEventsRequest {

    // Location-based search
    private Double latitude;
    private Double longitude;
    private Double radiusKm; // Search radius in kilometers

    // Category filter
    private UUID categoryId;

    // Date range filter
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // Price filter
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    // Status filter
    private String status; // PENDING, APPROVED, CANCELLED, COMPLETED

    // Verified events only
    private Boolean verifiedOnly;

    // Free events only
    private Boolean freeOnly;

    // Availability filter
    private Boolean availableTicketsOnly;

    // Text search
    private String keyword;

    // Pagination
    private Integer page = 0;
    private Integer size = 20;

    // Sorting
    private String sortBy = "startTime"; // startTime, createdAt, ticketPrice, distance
    private String sortDirection = "ASC"; // ASC, DESC
}
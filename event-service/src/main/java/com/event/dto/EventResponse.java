package com.event.dto;

import com.event.entity.EventStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventResponse {

    private UUID id;
    private String title;
    private String description;
    private CategoryResponse category;
    private UUID hostId;
    private String venue;
    private String address;
    private LocationResponse location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String imageUrl;
    private BigDecimal ticketPrice;
    private Integer ticketLimit;
    private Integer ticketsSold;
    private Integer remainingTickets;
    private Boolean hasAvailableTickets;
    private Boolean verified;
    private EventStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Optional fields for nearby search
    private Double distanceKm;
    private Boolean isSaved;
    private Boolean hasRsvp;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationResponse {
        private Double latitude;
        private Double longitude;
    }
}
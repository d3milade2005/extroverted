package com.event.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventDTO {
    private UUID id;
    private String title;
    private String description;
    private CategoryDTO category;

    // Host information
    private UUID hostId;

    // Venue information
    private String venue;
    private String address;
    private Location location;

    // Time information
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private Integer remainingTickets;

    // Ticket information
    private BigDecimal ticketPrice;
    private Integer ticketLimit;
    private Integer ticketsSold;

    // Metadata
    private String imageUrl;
    private Boolean verified;
    private String status;

    // Interaction counts (for popularity scoring)
    private Long viewCount;
    private Long saveCount;
    private Long rsvpCount;
    private Long shareCount;

    /**
     * Check if event has available tickets
     */
    public boolean hasAvailableTickets() {
        if (ticketLimit == null || ticketsSold == null) {
            return true;  // Assume available if not specified
        }
        return ticketsSold < ticketLimit;
    }

    /**
     * Calculate total interaction count (for popularity)
     */
    public long getTotalInteractions() {
        long total = 0;
        if (viewCount != null) total += viewCount;
        if (saveCount != null) total += saveCount;
        if (rsvpCount != null) total += rsvpCount;
        if (shareCount != null) total += shareCount;
        return total;
    }

    /**
     * Check if event is free
     */
    public boolean isFree() {
        return ticketPrice == null || ticketPrice.compareTo(BigDecimal.ZERO) == 0;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CategoryDTO {
        private UUID id;
        private String name;
        private String description;
        private String icon;
    }

    public String getCategoryName() {
        return category != null ? category.getName() : null;
    }
}

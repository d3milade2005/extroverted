package com.event.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEventRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String description;

    @Size(max = 255, message = "Venue must not exceed 255 characters")
    private String venue;

    private String address;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String imageUrl;

    @DecimalMin(value = "0.0", message = "Ticket price must be non-negative")
    private BigDecimal ticketPrice;

    @Min(value = 1, message = "Ticket limit must be at least 1")
    private Integer ticketLimit;
}
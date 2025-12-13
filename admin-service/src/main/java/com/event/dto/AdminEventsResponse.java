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
public class AdminEventsResponse {
    private UUID id;
    private String title;
    private String description;
    private String category;
    private UserBatchDTO host;
    private String venue;
    private String address;
    private Location location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    private BigDecimal ticketPrice;
    private Integer ticketLimit;
    private Integer ticketsSold;
    private String imageUrl;
    private Boolean verified;
    private String status;
}

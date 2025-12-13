package com.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveEventDTO {
    private UUID eventId;
    private String status;
    private AdminUser approvedBy;
    private LocalDateTime approvedAt;
}
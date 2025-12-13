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
public class RejectEventDTO {
    private UUID eventId;
    private String status;
    private AdminUser rejectedBy;
    private LocalDateTime rejectedAt;
    private String reason;
    private Boolean allowResubmit = true;
}

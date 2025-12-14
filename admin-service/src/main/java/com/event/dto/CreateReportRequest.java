package com.event.dto;

import com.event.entity.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateReportRequest {

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @NotNull(message = "Report reason is required")
    private ReportReason reportReason;

    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String detailedDescription;

    private List<String> evidenceUrls;
}


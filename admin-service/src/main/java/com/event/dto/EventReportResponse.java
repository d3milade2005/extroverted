package com.event.dto;

import com.event.admin.model.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class EventReportResponse {

    private UUID id;
    private UUID eventId;
    private UUID reportedBy;
    private ReportReason reportReason;
    private String detailedDescription;
    private ReportPriority priority;
    private ReportStatus status;
    private UUID assignedTo;
    private LocalDateTime assignedAt;
    private UUID resolvedBy;
    private LocalDateTime resolvedAt;
    private ResolutionAction resolutionAction;
    private String resolutionNotes;
    private List<String> evidenceUrls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EventReportResponse from(EventReport report) {
        return EventReportResponse.builder()
                .id(report.getId())
                .eventId(report.getEventId())
                .reportedBy(report.getReportedBy())
                .reportReason(report.getReportReason())
                .detailedDescription(report.getDetailedDescription())
                .priority(report.getPriority())
                .status(report.getStatus())
                .assignedTo(report.getAssignedTo())
                .assignedAt(report.getAssignedAt())
                .resolvedBy(report.getResolvedBy())
                .resolvedAt(report.getResolvedAt())
                .resolutionAction(report.getResolutionAction())
                .resolutionNotes(report.getResolutionNotes())
                .evidenceUrls(report.getEvidenceUrls())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
package com.event.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "event_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "reported_by", nullable = false)
    private UUID reportedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_reason", nullable = false, length = 50)
    private ReportReason reportReason;

    @Column(name = "detailed_description", columnDefinition = "TEXT")
    private String detailedDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private ReportPriority priority = ReportPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_action", length = 50)
    private ResolutionAction resolutionAction;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "evidence_urls", columnDefinition = "TEXT[]")
    private List<String> evidenceUrls;

    @Column(name = "reporter_ip", length = 45)
    private String reporterIp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    public void assignTo(UUID adminId) {
        this.assignedTo = adminId;
        this.assignedAt = LocalDateTime.now();
        if (this.status == ReportStatus.PENDING) {
            this.status = ReportStatus.INVESTIGATING;
        }
    }

    public void resolve(UUID adminId, ResolutionAction action, String notes) {
        this.resolvedBy = adminId;
        this.resolvedAt = LocalDateTime.now();
        this.resolutionAction = action;
        this.resolutionNotes = notes;
        this.status = ReportStatus.RESOLVED;
    }


    public void dismiss(UUID adminId, String reason) {
        this.resolvedBy = adminId;
        this.resolvedAt = LocalDateTime.now();
        this.resolutionAction = ResolutionAction.DISMISS_REPORT;
        this.resolutionNotes = reason;
        this.status = ReportStatus.DISMISSED;
    }


    public boolean isPending() {
        return status == ReportStatus.PENDING;
    }


    public boolean isAssigned() {
        return assignedTo != null;
    }

    public boolean isResolved() {
        return status == ReportStatus.RESOLVED || status == ReportStatus.DISMISSED;
    }
}
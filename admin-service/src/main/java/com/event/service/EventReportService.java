// WE WILL BE USING THE USERSERVICECLIENT HERE TO CHECK FOR A USER AND IF THAT USER IS AN ADMIN
// THE ADMIN USER REPO WILL BE DELETED AS WELL AS THE ADMINUSERSERVICE


package com.event.service;

import com.event.dto.CreateReportRequest;
import com.event.dto.ResolveReportRequest;
import com.event.exception.ResourceNotFoundException;
import com.event.exception.UnauthorizedException;
import com.event.entity.*;
import com.event.entity.EntityType;
import com.event.repository.EventReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventReportService {

    private final EventReportRepository reportRepository;
    private final AdminActionService adminActionService;
    private final AdminUserService adminUserService;

    @Transactional
    public ReportEvent createReport(CreateReportRequest request, UUID userId, String ipAddress) {
        log.info("Creating report for event {} by user {}", request.getEventId(), userId);

        // Check if user already reported this event
        if (reportRepository.existsByEventIdAndReportedBy(request.getEventId(), userId)) {
            throw new IllegalStateException("You have already reported this event");
        }

        ReportEvent report = ReportEvent.builder()
                .eventId(request.getEventId())
                .reportedBy(userId)
                .reportReason(request.getReportReason())
                .detailedDescription(request.getDetailedDescription())
                .evidenceUrls(request.getEvidenceUrls())
                .priority(request.getReportReason().getSuggestedPriority())
                .status(ReportStatus.PENDING)
                .reporterIp(ipAddress)
                .build();

        ReportEvent savedReport = reportRepository.save(report);
        log.info("Report {} created successfully", savedReport.getId());

        return savedReport;
    }

    @Transactional(readOnly = true)
    public ReportEvent getReportById(UUID reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + reportId));
    }

    @Transactional(readOnly = true)
    public Page<ReportEvent> getPendingReports(Pageable pageable) {
        return reportRepository.findPendingReports(pageable);
    }

    @Transactional(readOnly = true)
    public Page<ReportEvent> getReportsByStatus(ReportStatus status, Pageable pageable) {
        return reportRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ReportEvent> getMyAssignedReports(UUID adminId, Pageable pageable) {
        return reportRepository.findByAssignedTo(adminId, pageable);
    }

    @Transactional(readOnly = true)
    public List<ReportEvent> getReportsForEvent(UUID eventId) {
        return reportRepository.findByEventId(eventId);
    }

    @Transactional
    public ReportEvent assignReport(UUID reportId, UUID assignToAdminId, UUID currentAdminId) {
        log.info("Assigning report {} to admin {} by admin {}", reportId, assignToAdminId, currentAdminId);

        // Verify current admin has permission
        adminUserService.verifyPermission(currentAdminId, Permission.ASSIGN_REPORTS);

        // Verify target admin exists and is active
        adminUserService.getByUserId(assignToAdminId);

        ReportEvent report = getReportById(reportId);

        if (report.isResolved()) {
            throw new IllegalStateException("Cannot assign a resolved report");
        }

        report.assignTo(assignToAdminId);
        ReportEvent savedReport = reportRepository.save(report);

        // Log action
        adminActionService.logAction(
                ActionType.ASSIGN_REPORT,
                currentAdminId,
                EntityType.REPORT,
                reportId,
                "Assigned report to admin " + assignToAdminId,
                null
        );

        log.info("Report {} assigned successfully", reportId);
        return savedReport;
    }

    @Transactional
    public ReportEvent resolveReport(UUID reportId, ResolveReportRequest request, UUID adminId) {
        log.info("Resolving report {} by admin {}", reportId, adminId);

        // Verify admin has permission
        adminUserService.verifyPermission(adminId, Permission.MANAGE_REPORTS);

        ReportEvent report = getReportById(reportId);

        if (report.isResolved()) {
            throw new IllegalStateException("Report is already resolved");
        }

        report.resolve(adminId, request.getResolutionAction(), request.getResolutionNotes());
        ReportEvent savedReport = reportRepository.save(report);

        // Log action
        adminActionService.logAction(
                ActionType.RESOLVE_REPORT,
                adminId,
                EntityType.REPORT,
                reportId,
                "Resolved report with action: " + request.getResolutionAction(),
                null
        );

        log.info("Report {} resolved with action {}", reportId, request.getResolutionAction());
        return savedReport;
    }

    @Transactional
    public ReportEvent dismissReport(UUID reportId, String reason, UUID adminId) {
        log.info("Dismissing report {} by admin {}", reportId, adminId);

        // Verify admin has permission
        adminUserService.verifyPermission(adminId, Permission.MANAGE_REPORTS);

        ReportEvent report = getReportById(reportId);

        if (report.isResolved()) {
            throw new IllegalStateException("Report is already resolved");
        }

        report.dismiss(adminId, reason);
        ReportEvent savedReport = reportRepository.save(report);

        // Log action
        adminActionService.logAction(
                ActionType.DISMISS_REPORT,
                adminId,
                EntityType.REPORT,
                reportId,
                "Dismissed report: " + reason,
                null
        );

        log.info("Report {} dismissed", reportId);
        return savedReport;
    }

    // Analytics
    @Transactional(readOnly = true)
    public long countPendingReports() {
        return reportRepository.countPendingByPriority(ReportPriority.MEDIUM.name()) +
                reportRepository.countPendingByPriority(ReportPriority.HIGH.name()) +
                reportRepository.countPendingByPriority(ReportPriority.CRITICAL.name()) +
                reportRepository.countPendingByPriority(ReportPriority.LOW.name());
    }

    @Transactional(readOnly = true)
    public long countReportsCreatedToday() {
        return reportRepository.countCreatedToday(LocalDateTime.now().toLocalDate().atStartOfDay());
    }

    @Transactional(readOnly = true)
    public long countReportsResolvedToday() {
        return reportRepository.countResolvedToday(LocalDateTime.now().toLocalDate().atStartOfDay());
    }
}
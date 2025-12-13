package com.cityvibe.admin.controller;

import com.cityvibe.admin.dto.request.CreateReportRequest;
import com.cityvibe.admin.dto.request.ResolveReportRequest;
import com.cityvibe.admin.dto.response.EventReportResponse;
import com.cityvibe.admin.model.EventReport;
import com.cityvibe.admin.model.ReportStatus;
import com.cityvibe.admin.service.EventReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class EventReportController {

    private final EventReportService reportService;

    @PostMapping
    public ResponseEntity<EventReportResponse> createReport(
            @Valid @RequestBody CreateReportRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String ipAddress = httpRequest.getRemoteAddr();

        EventReport report = reportService.createReport(request, userId, ipAddress);
        return ResponseEntity.status(HttpStatus.CREATED).body(EventReportResponse.from(report));
    }

    @GetMapping("/pending")
    public ResponseEntity<Page<EventReportResponse>> getPendingReports(Pageable pageable) {
        Page<EventReport> reports = reportService.getPendingReports(pageable);
        return ResponseEntity.ok(reports.map(EventReportResponse::from));
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<EventReportResponse> getReportById(@PathVariable UUID reportId) {
        EventReport report = reportService.getReportById(reportId);
        return ResponseEntity.ok(EventReportResponse.from(report));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<EventReportResponse>> getReportsByStatus(
            @PathVariable ReportStatus status,
            Pageable pageable
    ) {
        Page<EventReport> reports = reportService.getReportsByStatus(status, pageable);
        return ResponseEntity.ok(reports.map(EventReportResponse::from));
    }

    @GetMapping("/my-assigned")
    public ResponseEntity<Page<EventReportResponse>> getMyAssignedReports(
            @AuthenticationPrincipal Jwt jwt,
            Pageable pageable
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        Page<EventReport> reports = reportService.getMyAssignedReports(adminId, pageable);
        return ResponseEntity.ok(reports.map(EventReportResponse::from));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<EventReportResponse>> getReportsForEvent(@PathVariable UUID eventId) {
        List<EventReport> reports = reportService.getReportsForEvent(eventId);
        List<EventReportResponse> responses = reports.stream()
                .map(EventReportResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{reportId}/assign/{adminId}")
    public ResponseEntity<EventReportResponse> assignReport(
            @PathVariable UUID reportId,
            @PathVariable UUID adminId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID currentAdminId = UUID.fromString(jwt.getSubject());
        EventReport report = reportService.assignReport(reportId, adminId, currentAdminId);
        return ResponseEntity.ok(EventReportResponse.from(report));
    }

    @PostMapping("/{reportId}/resolve")
    public ResponseEntity<EventReportResponse> resolveReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody ResolveReportRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        EventReport report = reportService.resolveReport(reportId, request, adminId);
        return ResponseEntity.ok(EventReportResponse.from(report));
    }

    @PostMapping("/{reportId}/dismiss")
    public ResponseEntity<EventReportResponse> dismissReport(
            @PathVariable UUID reportId,
            @RequestParam String reason,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        EventReport report = reportService.dismissReport(reportId, reason, adminId);
        return ResponseEntity.ok(EventReportResponse.from(report));
    }
}
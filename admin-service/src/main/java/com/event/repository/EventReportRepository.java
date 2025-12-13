package com.event.repository;

import com.event.entity.ReportEvent;
import com.event.entity.AdminAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventReportRepository extends JpaRepository<ReportEvent, UUID>{
    Page<ReportEvent> findByStatus(AdminAction status, Pageable pageable);

    // Find by event
    List<ReportEvent> findByEventId(UUID eventId);

    // Find pending reports
    @Query("SELECT r FROM ReportEvent r WHERE r.status = 'PENDING' ORDER BY r.priority DESC, r.createdAt ASC")
    Page<ReportEvent> findPendingReports(Pageable pageable);

    // Find assigned reports for admin
    Page<ReportEvent> findByAssignedTo(UUID adminId, Pageable pageable);

    // Count pending by priority
    @Query("SELECT COUNT(r) FROM ReportEvent r WHERE r.status = 'PENDING' AND r.priority = :priority")
    long countPendingByPriority(@Param("priority") String priority);

    // Find reports for event by user
    boolean existsByEventIdAndReportedBy(UUID eventId, UUID userId);

    // Analytics - reports created today
    @Query("SELECT COUNT(r) FROM ReportEvent r WHERE r.createdAt >= :startOfDay")
    long countCreatedToday(@Param("startOfDay") LocalDateTime startOfDay);

    // Analytics - reports resolved today
    @Query("SELECT COUNT(r) FROM ReportEvent r WHERE r.status IN ('RESOLVED', 'DISMISSED') AND r.resolvedAt >= :startOfDay")
    long countResolvedToday(@Param("startOfDay") LocalDateTime startOfDay);
}

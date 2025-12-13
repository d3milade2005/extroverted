package com.event.repository;

import com.event.entity.AdminAction;
import com.event.entity.ActionType;
import com.event.entity.EntityType;
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
public interface AdminActionRepository extends JpaRepository<AdminAction, UUID> {
    Page<AdminAction> findByPerformedBy(UUID adminId, Pageable pageable);

    // Find actions by type
    Page<AdminAction> findByActionType(ActionType actionType, Pageable pageable);

    // Find actions for specific entity
    List<AdminAction> findByTargetEntityTypeAndTargetEntityId(EntityType entityType, UUID entityId);

    // Find recent actions (last 30 days)
    @Query("SELECT a FROM AdminAction a WHERE a.createdAt >= :since ORDER BY a.createdAt DESC")
    Page<AdminAction> findRecentActions(@Param("since") LocalDateTime since, Pageable pageable);

    // Count actions by admin today
    @Query("SELECT COUNT(a) FROM AdminAction a WHERE a.performedBy = :adminId AND a.createdAt >= :startOfDay")
    long countByAdminToday(@Param("adminId") UUID adminId, @Param("startOfDay") LocalDateTime startOfDay);

    // Analytics - action count by type
    @Query("SELECT a.actionType, COUNT(a) FROM AdminAction a WHERE a.createdAt >= :since GROUP BY a.actionType")
    List<Object[]> countByActionTypeSince(@Param("since") LocalDateTime since);
}

package com.cityvibe.admin.service;

import com.cityvibe.admin.model.ActionType;
import com.cityvibe.admin.model.AdminAction;
import com.cityvibe.admin.model.EntityType;
import com.cityvibe.admin.repository.AdminActionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminActionService {

    private final AdminActionRepository actionRepository;

    @Transactional
    public AdminAction logAction(
            ActionType actionType,
            UUID performedBy,
            EntityType entityType,
            UUID entityId,
            String description,
            Map<String, Object> metadata
    ) {
        AdminAction action = AdminAction.builder()
                .actionType(actionType)
                .performedBy(performedBy)
                .targetEntityType(entityType)
                .targetEntityId(entityId)
                .actionDescription(description)
                .actionMetadata(metadata)
                .build();

        AdminAction saved = actionRepository.save(action);
        log.info("Logged action: {} by admin {} on {} {}", actionType, performedBy, entityType, entityId);

        return saved;
    }

    @Transactional(readOnly = true)
    public Page<AdminAction> getActionsByAdmin(UUID adminId, Pageable pageable) {
        return actionRepository.findByPerformedBy(adminId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AdminAction> getRecentActions(int days, Pageable pageable) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return actionRepository.findRecentActions(since, pageable);
    }

    @Transactional(readOnly = true)
    public long countAdminActionsToday(UUID adminId) {
        return actionRepository.countByAdminToday(adminId, LocalDateTime.now().toLocalDate().atStartOfDay());
    }
}
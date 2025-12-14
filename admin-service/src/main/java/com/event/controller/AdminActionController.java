package com.event.controller;

import com.event.dto.AdminActionResponse;
import com.event.entity.ActionType;
import com.event.entity.EntityType;
import com.event.entity.AdminAction;
import com.event.service.AdminActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/actions")
@RequiredArgsConstructor
public class AdminActionController {

    private final AdminActionService actionService;

    @GetMapping("/my-actions")
    public ResponseEntity<Page<AdminActionResponse>> getMyActions(
            @AuthenticationPrincipal Jwt jwt,
            Pageable pageable
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        Page<AdminAction> actions = actionService.getActionsByAdmin(adminId, pageable);
        return ResponseEntity.ok(actions.map(AdminActionResponse::from));
    }

    @GetMapping("/recent")
    public ResponseEntity<Page<AdminActionResponse>> getRecentActions(
            @RequestParam(defaultValue = "30") int days,
            Pageable pageable
    ) {
        Page<AdminAction> actions = actionService.getRecentActions(days, pageable);
        return ResponseEntity.ok(actions.map(AdminActionResponse::from));
    }

    @GetMapping("/actionType/{actionType}")
    public ResponseEntity<Page<AdminActionResponse>> getActionsByType(
            @PathVariable ActionType actionType,
            Pageable pageable
    ) {
        Page<AdminAction> actions = actionService.getActionType(actionType, pageable);
        return ResponseEntity.ok(actions.map(AdminActionResponse::from));
    }

    @GetMapping("/entityType/{entityType}")
    public ResponseEntity<Page<AdminActionResponse>> getEntityByType(
            @PathVariable EntityType entityType,
            Pageable pageable
    ) {
        Page<AdminAction> actions = actionService.getEntityType(entityType, pageable);
        return ResponseEntity.ok(actions.map(AdminActionResponse::from));
    }

    @GetMapping("/entity/{entityId}")
    public ResponseEntity<Page<AdminActionResponse>> getEntityById(
            @PathVariable UUID entityId,
            Pageable pageable
    ) {
        Page<AdminAction> actions = actionService.getEntityId(entityId, pageable);
        return ResponseEntity.ok(actions.map(AdminActionResponse::from));
    }

    @GetMapping("/entity/{entityId}/{entityType}")
    public ResponseEntity<Page<AdminActionResponse>> getEntityByTypeAndId(
            @PathVariable UUID entityId,
            @PathVariable EntityType entityType,
            Pageable pageable
    ) {
        Page<AdminAction> actions = actionService.getEntityByTypeAndId(entityId, entityType, pageable);
        return ResponseEntity.ok(actions.map(AdminActionResponse::from));
    }
}
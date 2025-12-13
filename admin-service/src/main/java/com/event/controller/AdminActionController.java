package com.cityvibe.admin.controller;

import com.cityvibe.admin.dto.response.AdminActionResponse;
import com.cityvibe.admin.model.AdminAction;
import com.cityvibe.admin.service.AdminActionService;
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
}
package com.event.controller;

import com.event.dto.AdminEventsResponse;
import com.event.dto.EventDTO;
import com.event.service.AdminService;
import com.event.service.EventServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final EventServiceClient eventServiceClient;

    @GetMapping("/events")
    public ResponseEntity<List<AdminEventsResponse>> getEvents(@RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        List<AdminEventsResponse> events = adminService.getEvents(status, page, size);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{eventId}/approve")
    public ResponseEntity<?> approveEvent(@PathVariable UUID eventId, Authentication authentication) {
        UUID adminUserId = extractUserId(authentication);
        String extractedAdminName = extractAdminName(authentication);
        return ResponseEntity.ok(adminService.approveEvent(eventId, adminUserId, extractedAdminName));
    }

    @GetMapping("/{eventId}/reject")
    public ResponseEntity<?> rejectEvent(@PathVariable UUID eventId, Authentication authentication) {
        UUID adminUserId = extractUserId(authentication);
        String extractedAdminName = extractAdminName(authentication);
        return ResponseEntity.ok(adminService.rejectEvent(eventId, adminUserId, extractedAdminName));
    }


    private UUID extractUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return UUID.fromString(jwt.getSubject());
    }

    private String extractAdminName(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");
        return firstName + " " + lastName;
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}

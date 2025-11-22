package com.event.controller;

import com.event.dto.InitializeUserRequest;
import com.event.dto.UpdateLocationRequest;
import com.event.dto.UpdatePreferencesRequest;
import com.event.dto.UserProfileResponse;
import com.event.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @PostMapping("/initialize")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> initializeUser(@Valid @RequestBody InitializeUserRequest initializeUserRequest, Authentication authentication) {
        try {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String keycloakId = jwt.getSubject();
            String username = jwt.getClaimAsString("preferred_username");
            String email = jwt.getClaimAsString("email");
            String firstName = jwt.getClaimAsString("given_name");
            String lastName = jwt.getClaimAsString("family_name");

            UserProfileResponse response = userService.initializeUser(keycloakId, username, email, firstName, lastName, initializeUserRequest);

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "User initialization failed", "message", e.getMessage()));
        }
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            Jwt jwt = (Jwt) authentication.getPrincipal();

            String keycloakId = jwt.getSubject();

            UserProfileResponse response = userService.getUserProfile(keycloakId);

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found", "message", e.getMessage()));
        }
    }

    @PatchMapping("/me/preferences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updatePreferences(@Valid @RequestBody UpdatePreferencesRequest updatePreferencesRequest, Authentication authentication) {
        try {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String keycloakId = jwt.getSubject();
            UserProfileResponse response = userService.updatePreferences(keycloakId, updatePreferencesRequest);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to update preferences", "message", e.getMessage()));
        }
    }

    @PatchMapping("/me/location")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateLocation(@Valid @RequestBody UpdateLocationRequest request, Authentication authentication) {
        try {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String keycloakId = jwt.getSubject();

            UserProfileResponse response = userService.updateLocation(keycloakId, request);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to update location", "message", e.getMessage()));
        }
    }

    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteAccount(Authentication authentication) {
        try {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String keycloakId = jwt.getSubject();

            userService.deleteUser(keycloakId);
            return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to delete account", "message", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "user-service",
                "timestamp", System.currentTimeMillis()
        ));
    }
}

package com.event.controller;

import com.event.dto.*;
import com.event.entity.User;
import com.event.repository.UserRepository;
import com.event.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;

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


    @PostMapping("/users/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserBatchDTO> getUsersByIds(@RequestBody List<UUID> userIds) {
        return userRepository.findAllById(userIds).stream()
                .map(user -> new UserBatchDTO(user.getId(), user.getFirstName() + " " + user.getLastName(), user.getEmail()))
                .collect(Collectors.toList());
    }

    @GetMapping("/allUsers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserProfileResponse>> getUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) Boolean active,
            Pageable pageable
    ) {
        return ResponseEntity.ok(userService.getAllUsers(query, verified, active, pageable));
    }

    @PutMapping("/{id}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> banUser(
            @PathVariable UUID id,
            @RequestBody @Valid BanReason reason
    ) {
        userService.banUser(id, reason);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/unban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unbanUser(@PathVariable UUID id) {
        userService.unbanUser(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> verifyUser(@PathVariable UUID id) {
        userService.toggleVerification(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable UUID userId) {
        UserProfileResponse userProfileResponse = userService.getUser(userId);
        return ResponseEntity.ok(userProfileResponse);
    }

    @GetMapping("/user/{userId}/is-active-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Boolean> isActiveAdmin(@PathVariable UUID userId) {
        boolean isActiveAdmin = userService.isActiveAdmin(userId);
        return ResponseEntity.ok(isActiveAdmin);
    }

    @GetMapping("/admins")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserProfileResponse>> getAllAdmins(Pageable pageable) {
        Page<UserProfileResponse> admins = userService.getAllAdmins(pageable);
        return ResponseEntity.ok(admins);
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

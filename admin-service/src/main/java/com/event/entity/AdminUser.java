package com.cityvibe.admin.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing admin-specific metadata and permissions.
 * Note: Authentication is handled by Keycloak - this stores additional admin info only.
 *
 * @author CityVibe Team
 * @version 1.0
 */
@Entity
@Table(name = "admin_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // =====================================================
    // User Reference (from User Service / Keycloak)
    // =====================================================

    @Column(name = "user_id", unique = true, nullable = false)
    private UUID userId;

    // =====================================================
    // Admin Details
    // =====================================================

    @Enumerated(EnumType.STRING)
    @Column(name = "admin_role", nullable = false, length = 50)
    private AdminRole adminRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "department", length = 100)
    private Department department;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "admin_permissions", joinColumns = @JoinColumn(name = "admin_user_id"))
    @Column(name = "permission")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private List<Permission> permissions = new ArrayList<>();

    // =====================================================
    // Status
    // =====================================================

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // =====================================================
    // Activity Tracking
    // =====================================================

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_action_at")
    private LocalDateTime lastActionAt;

    @Column(name = "total_actions_count", nullable = false)
    @Builder.Default
    private Integer totalActionsCount = 0;

    // =====================================================
    // Admin Management
    // =====================================================

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "deactivated_by")
    private UUID deactivatedBy;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @Column(name = "deactivation_reason", columnDefinition = "TEXT")
    private String deactivationReason;

    // =====================================================
    // Additional Info
    // =====================================================

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // =====================================================
    // Metadata
    // =====================================================

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // =====================================================
    // Helper Methods
    // =====================================================

    /**
     * Update last login timestamp
     */
    public void recordLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * Record that an action was performed
     */
    public void recordAction() {
        this.lastActionAt = LocalDateTime.now();
        this.totalActionsCount++;
    }

    /**
     * Deactivate this admin account
     */
    public void deactivate(UUID deactivatedByAdmin, String reason) {
        this.isActive = false;
        this.deactivatedBy = deactivatedByAdmin;
        this.deactivatedAt = LocalDateTime.now();
        this.deactivationReason = reason;
    }

    /**
     * Reactivate this admin account
     */
    public void reactivate() {
        this.isActive = true;
        this.deactivatedBy = null;
        this.deactivatedAt = null;
        this.deactivationReason = null;
    }

    /**
     * Check if admin has a specific permission
     */
    public boolean hasPermission(Permission permission) {
        // SUPER_ADMIN has all permissions
        if (adminRole == AdminRole.SUPER_ADMIN) {
            return true;
        }
        return permissions != null && permissions.contains(permission);
    }

    /**
     * Check if admin has any of the specified permissions
     */
    public boolean hasAnyPermission(Permission... requiredPermissions) {
        if (adminRole == AdminRole.SUPER_ADMIN) {
            return true;
        }
        for (Permission permission : requiredPermissions) {
            if (hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if admin has all of the specified permissions
     */
    public boolean hasAllPermissions(Permission... requiredPermissions) {
        if (adminRole == AdminRole.SUPER_ADMIN) {
            return true;
        }
        for (Permission permission : requiredPermissions) {
            if (!hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Add a permission to this admin
     */
    public void addPermission(Permission permission) {
        if (permissions == null) {
            permissions = new ArrayList<>();
        }
        if (!permissions.contains(permission)) {
            permissions.add(permission);
        }
    }

    /**
     * Remove a permission from this admin
     */
    public void removePermission(Permission permission) {
        if (permissions != null) {
            permissions.remove(permission);
        }
    }

    /**
     * Get default permissions for a role
     */
    public static List<Permission> getDefaultPermissionsForRole(AdminRole role) {
        return switch (role) {
            case SUPER_ADMIN -> List.of(Permission.values()); // All permissions
            case ADMIN -> List.of(
                    Permission.APPROVE_EVENTS,
                    Permission.REJECT_EVENTS,
                    // Permission.VERIFY_PROMOTERS, // Commented out for KYC
                    Permission.MANAGE_REPORTS,
                    Permission.BAN_USERS,
                    Permission.VIEW_ANALYTICS
            );
            case MODERATOR -> List.of(
                    Permission.APPROVE_EVENTS,
                    Permission.REJECT_EVENTS,
                    Permission.MANAGE_REPORTS,
                    Permission.VIEW_ANALYTICS
            );
        };
    }
}
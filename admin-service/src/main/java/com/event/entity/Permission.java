package com.cityvibe.admin.model;

/**
 * Enum representing specific permissions that can be granted to admins.
 * Fine-grained permissions for controlling what actions admins can perform.
 *
 * @author CityVibe Team
 * @version 1.0
 */
public enum Permission {

    // =====================================================
    // Event Management Permissions
    // =====================================================

    /**
     * Can approve pending events
     */
    APPROVE_EVENTS("Approve Events", "Can approve pending events for publication"),

    /**
     * Can reject pending events
     */
    REJECT_EVENTS("Reject Events", "Can reject pending events"),

    /**
     * Can suspend active events
     */
    SUSPEND_EVENTS("Suspend Events", "Can suspend active events"),

    /**
     * Can delete events
     */
    DELETE_EVENTS("Delete Events", "Can permanently delete events"),

    /**
     * Can edit event information
     */
    EDIT_EVENTS("Edit Events", "Can modify event information"),

    // =====================================================
    // Promoter/KYC Permissions (Commented out for now)
    // =====================================================

    // /**
    //  * Can verify promoter KYC documents
    //  */
    // VERIFY_PROMOTERS("Verify Promoters", "Can approve/reject promoter verifications"),

    // =====================================================
    // Report Management Permissions
    // =====================================================

    /**
     * Can manage user reports
     */
    MANAGE_REPORTS("Manage Reports", "Can view, assign, and resolve user reports"),

    /**
     * Can assign reports to other admins
     */
    ASSIGN_REPORTS("Assign Reports", "Can assign reports to other admins"),

    // =====================================================
    // User Management Permissions
    // =====================================================

    /**
     * Can ban users from platform
     */
    BAN_USERS("Ban Users", "Can ban users from the platform"),

    /**
     * Can unban previously banned users
     */
    UNBAN_USERS("Unban Users", "Can unban users"),

    /**
     * Can warn users
     */
    WARN_USERS("Warn Users", "Can issue warnings to users"),

    /**
     * Can view detailed user information
     */
    VIEW_USER_DETAILS("View User Details", "Can view detailed user information"),

    // =====================================================
    // Analytics & Reporting Permissions
    // =====================================================

    /**
     * Can view analytics dashboards
     */
    VIEW_ANALYTICS("View Analytics", "Can view analytics and reports"),

    /**
     * Can export analytics data
     */
    EXPORT_ANALYTICS("Export Analytics", "Can export analytics data"),

    // =====================================================
    // Admin Management Permissions
    // =====================================================

    /**
     * Can manage other admins (typically SUPER_ADMIN only)
     */
    MANAGE_ADMINS("Manage Admins", "Can create, update, and deactivate admin users"),

    /**
     * Can view audit logs
     */
    VIEW_AUDIT_LOGS("View Audit Logs", "Can view complete audit trail");

    private final String displayName;
    private final String description;

    Permission(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get the category this permission belongs to
     */
    public PermissionCategory getCategory() {
        return switch (this) {
            case APPROVE_EVENTS, REJECT_EVENTS, SUSPEND_EVENTS,
                 DELETE_EVENTS, EDIT_EVENTS -> PermissionCategory.EVENT_MANAGEMENT;
            // case VERIFY_PROMOTERS -> PermissionCategory.PROMOTER_VERIFICATION;
            case MANAGE_REPORTS, ASSIGN_REPORTS -> PermissionCategory.REPORT_MANAGEMENT;
            case BAN_USERS, UNBAN_USERS, WARN_USERS,
                 VIEW_USER_DETAILS -> PermissionCategory.USER_MANAGEMENT;
            case VIEW_ANALYTICS, EXPORT_ANALYTICS -> PermissionCategory.ANALYTICS;
            case MANAGE_ADMINS, VIEW_AUDIT_LOGS -> PermissionCategory.ADMIN_MANAGEMENT;
        };
    }

    /**
     * Check if this is a critical permission (should be restricted)
     */
    public boolean isCritical() {
        return switch (this) {
            case DELETE_EVENTS, BAN_USERS, MANAGE_ADMINS -> true;
            default -> false;
        };
    }

    /**
     * Permission categories for grouping
     */
    public enum PermissionCategory {
        EVENT_MANAGEMENT,
        PROMOTER_VERIFICATION,
        REPORT_MANAGEMENT,
        USER_MANAGEMENT,
        ANALYTICS,
        ADMIN_MANAGEMENT
    }
}
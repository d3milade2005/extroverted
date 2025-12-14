package com.event.entity;


public enum ActionType {
    APPROVE_EVENT("Approve Event", "Admin approved a pending event"),
    REJECT_EVENT("Reject Event", "Admin rejected a pending event"),
    SUSPEND_EVENT("Suspend Event", "Admin suspended an active event"),
    DELETE_EVENT("Delete Event", "Admin deleted an event"),
    UPDATE_EVENT("Update Event", "Admin updated event information"),

    // =====================================================
    // Promoter/KYC Actions (for future use)
    // =====================================================

    // APPROVE_PROMOTER("Approve Promoter", "Admin verified and approved a promoter"),
    // REJECT_PROMOTER("Reject Promoter", "Admin rejected a promoter verification"),

    // =====================================================
    // Report Management Actions
    // =====================================================

    RESOLVE_REPORT("Resolve Report", "Admin resolved a user report"),
    DISMISS_REPORT("Dismiss Report", "Admin dismissed a user report as unfounded"),
    ASSIGN_REPORT("Assign Report", "Admin assigned a report to someone"),

    // =====================================================
    // User Management Actions
    // =====================================================

    BAN_USER("Ban User", "Admin banned a user from the platform"),
    UNBAN_USER("Unban User", "Admin unbanned a previously banned user"),
    WARN_USER("Warn User", "Admin issued a warning to a user"),

    // =====================================================
    // Admin Management Actions
    // =====================================================

    CREATE_ADMIN("Create Admin", "Created a new admin user"),
    UPDATE_ADMIN("Update Admin", "Updated admin user details"),
    DEACTIVATE_ADMIN("Deactivate Admin", "Deactivated an admin user"),
    UPDATE_ADMIN_PERMISSIONS("Update Permissions", "Updated admin permissions"),

    // =====================================================
    // Other Actions
    // =====================================================

    OTHER("Other", "Other administrative action");

    private final String displayName;
    private final String description;

    ActionType(String displayName, String description) {
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
     * Get the category of this action type
     */
    public ActionCategory getCategory() {
        return switch (this) {
            case APPROVE_EVENT, REJECT_EVENT, SUSPEND_EVENT, DELETE_EVENT, UPDATE_EVENT -> ActionCategory.EVENT_MANAGEMENT;
            // case APPROVE_PROMOTER, REJECT_PROMOTER -> ActionCategory.PROMOTER_VERIFICATION;
            case RESOLVE_REPORT, DISMISS_REPORT, ASSIGN_REPORT -> ActionCategory.REPORT_MANAGEMENT;
            case BAN_USER, UNBAN_USER, WARN_USER -> ActionCategory.USER_MANAGEMENT;
            case CREATE_ADMIN, UPDATE_ADMIN, DEACTIVATE_ADMIN, UPDATE_ADMIN_PERMISSIONS -> ActionCategory.ADMIN_MANAGEMENT;
            default -> ActionCategory.OTHER;
        };
    }

    /**
     * Check if this action requires a reason to be provided
     */
    public boolean requiresReason() {
        return switch (this) {
            case REJECT_EVENT, SUSPEND_EVENT, DELETE_EVENT,
                 BAN_USER, DEACTIVATE_ADMIN, DISMISS_REPORT -> true;
            default -> false;
        };
    }

    /**
     * Check if this is a critical action (requires SUPER_ADMIN or special permissions)
     */
    public boolean isCritical() {
        return switch (this) {
            case DELETE_EVENT, BAN_USER, DEACTIVATE_ADMIN -> true;
            default -> false;
        };
    }

    /**
     * Category grouping for action types
     */
    public enum ActionCategory {
        EVENT_MANAGEMENT,
        PROMOTER_VERIFICATION,
        REPORT_MANAGEMENT,
        USER_MANAGEMENT,
        ADMIN_MANAGEMENT,
        OTHER
    }
}
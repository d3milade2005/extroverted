package com.event.entity;


public enum EntityType {

    /**
     * Action targets an event
     */
    EVENT("Event", "events"),

    /**
     * Action targets a user account
     */
    USER("User", "users"),

    /**
     * Action targets a promoter/host verification (for future KYC)
     */
    PROMOTER("Promoter", "promoters"),

    /**
     * Action targets a report
     */
    REPORT("Report", "reports"),

    /**
     * Action targets an admin user
     */
    ADMIN("Admin", "admins"),

    /**
     * Action targets something else
     */
    OTHER("Other", "other");

    private final String displayName;
    private final String pluralName;

    EntityType(String displayName, String pluralName) {
        this.displayName = displayName;
        this.pluralName = pluralName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPluralName() {
        return pluralName;
    }

    /**
     * Get the service URL path segment for this entity type
     * Used for constructing URLs to view the entity
     */
    public String getServicePath() {
        return switch (this) {
            case EVENT -> "/api/events";
            case USER -> "/api/users";
            case PROMOTER -> "/api/host";
            case REPORT -> "/api/admin/reports";
            case ADMIN -> "/api/admin/users";
            default -> "/api";
        };
    }
}
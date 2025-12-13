package com.cityvibe.admin.model;

/**
 * Enum representing the department an admin belongs to.
 * Used for organizing admin responsibilities and reporting.
 *
 * @author CityVibe Team
 * @version 1.0
 */
public enum Department {

    /**
     * Content Moderation - Reviews and moderates events and reports
     */
    CONTENT_MODERATION("Content Moderation", "Reviews events and user reports"),

    /**
     * User Management - Handles user accounts, bans, and support
     */
    USER_MANAGEMENT("User Management", "Manages user accounts and support"),

    /**
     * Event Management - Works directly with event hosts and quality
     */
    EVENT_MANAGEMENT("Event Management", "Oversees event quality and hosts"),

    /**
     * General - Not assigned to a specific department
     */
    GENERAL("General", "General administrative duties"),

    /**
     * Other - Custom department
     */
    OTHER("Other", "Other department");

    private final String displayName;
    private final String description;

    Department(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
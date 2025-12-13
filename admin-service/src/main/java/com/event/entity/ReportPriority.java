package com.cityvibe.admin.model;

/**
 * Enum representing the priority level of a report.
 * Priority determines how urgently the report should be reviewed.
 *
 * @author CityVibe Team
 * @version 1.0
 */
public enum ReportPriority {

    /**
     * Low priority - can be reviewed when time permits
     * Examples: Minor information errors, duplicate events
     */
    LOW("Low", 1),

    /**
     * Medium priority - should be reviewed within a few days
     * Examples: Potentially fake events, inappropriate content
     */
    MEDIUM("Medium", 2),

    /**
     * High priority - should be reviewed within 24 hours
     * Examples: Suspected scams, safety concerns
     */
    HIGH("High", 3),

    /**
     * Critical priority - immediate attention required
     * Examples: Multiple reports of same event, severe safety issues
     */
    CRITICAL("Critical", 4);

    private final String displayName;
    private final int level;

    ReportPriority(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Get SLA in hours for this priority level
     */
    public int getSlaHours() {
        return switch (this) {
            case CRITICAL -> 4;   // 4 hours
            case HIGH -> 24;      // 24 hours
            case MEDIUM -> 72;    // 3 days
            case LOW -> 168;      // 7 days
        };
    }

    /**
     * Check if this priority is higher than another
     */
    public boolean isHigherThan(ReportPriority other) {
        return this.level > other.level;
    }

    /**
     * Escalate priority by one level (with max at CRITICAL)
     */
    public ReportPriority escalate() {
        return switch (this) {
            case LOW -> MEDIUM;
            case MEDIUM -> HIGH;
            case HIGH, CRITICAL -> CRITICAL;
        };
    }
}
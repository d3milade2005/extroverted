package com.event.entity;

/**
 * Enum representing the status of a report in the investigation workflow.
 *
 * @author CityVibe Team
 * @version 1.0
 */
public enum ReportStatus {

    /**
     * Report has been submitted but not yet reviewed or assigned
     */
    PENDING("Pending", "Awaiting review"),

    /**
     * Report has been assigned to an admin and is being investigated
     */
    INVESTIGATING("Investigating", "Currently under investigation"),

    /**
     * Report has been investigated and action taken
     */
    RESOLVED("Resolved", "Investigation complete, action taken"),

    /**
     * Report was reviewed but no action needed
     */
    DISMISSED("Dismissed", "Reviewed and dismissed");

    private final String displayName;
    private final String description;

    ReportStatus(String displayName, String description) {
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
     * Check if report is in a final state (resolved or dismissed)
     */
    public boolean isFinal() {
        return this == RESOLVED || this == DISMISSED;
    }

    /**
     * Check if report is still active (not resolved)
     */
    public boolean isActive() {
        return !isFinal();
    }
}
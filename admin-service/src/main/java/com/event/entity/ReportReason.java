package com.cityvibe.admin.model;

/**
 * Enum representing the reason for reporting an event.
 * These are predefined categories users can select when reporting problematic events.
 *
 * @author CityVibe Team
 * @version 1.0
 */
public enum ReportReason {

    /**
     * Event appears to be fake or non-existent
     */
    FAKE_EVENT("Fake Event", "Event appears to be fake or does not exist"),

    /**
     * Event seems to be a scam (taking money without delivering)
     */
    SCAM_SUSPECTED("Scam Suspected", "Event appears to be fraudulent or a scam"),

    /**
     * Event contains inappropriate or offensive content
     */
    INAPPROPRIATE_CONTENT("Inappropriate Content", "Event contains offensive or inappropriate material"),

    /**
     * Event is a duplicate of another existing event
     */
    DUPLICATE_EVENT("Duplicate Event", "This event is a duplicate of another listing"),

    /**
     * Event information is incorrect or misleading
     */
    WRONG_INFORMATION("Wrong Information", "Event details are incorrect or misleading"),

    /**
     * Event was cancelled but listing not updated
     */
    CANCELLED_NOT_UPDATED("Not Updated After Cancellation", "Event was cancelled but still showing as active"),

    /**
     * Event poses safety concerns
     */
    SAFETY_CONCERNS("Safety Concerns", "Event may pose safety or security risks"),

    /**
     * Event is spam or low-quality
     */
    SPAM("Spam", "Event appears to be spam or promotional clutter"),

    /**
     * Other reason not covered by predefined categories
     */
    OTHER("Other", "Other reason (see description)");

    private final String displayName;
    private final String description;

    ReportReason(String displayName, String description) {
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
     * Get priority suggestion based on report reason
     */
    public ReportPriority getSuggestedPriority() {
        return switch (this) {
            case SCAM_SUSPECTED, SAFETY_CONCERNS -> ReportPriority.HIGH;
            case FAKE_EVENT, INAPPROPRIATE_CONTENT -> ReportPriority.MEDIUM;
            default -> ReportPriority.LOW;
        };
    }
}
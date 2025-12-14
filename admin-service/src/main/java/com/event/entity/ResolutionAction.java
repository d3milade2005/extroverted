package com.event.entity;

/**
 * Enum representing the action taken to resolve a report.
 * This indicates what the admin decided to do after investigating.
 *
 * @author CityVibe Team
 * @version 1.0
 */
public enum ResolutionAction {

    /**
     * Event was removed from platform
     */
    REMOVE_EVENT("Remove Event", "Event has been removed from the platform"),

    /**
     * Event host was warned but event remains
     */
    WARN_HOST("Warn Host", "Host has been warned, event remains active"),

    /**
     * Event information was corrected
     */
    UPDATE_EVENT("Update Event", "Event information has been corrected"),

    /**
     * Report was unfounded, no action taken
     */
    DISMISS_REPORT("Dismiss Report", "Report was reviewed and found to be unfounded"),

    /**
     * Event host was banned from platform
     */
    BAN_HOST("Ban Host", "Event host has been banned from the platform"),

    /**
     * Event was temporarily suspended pending investigation
     */
    SUSPEND_EVENT("Suspend Event", "Event has been temporarily suspended");

    private final String displayName;
    private final String description;

    ResolutionAction(String displayName, String description) {
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
     * Check if this action is severe (affects host negatively)
     */
    public boolean isSevere() {
        return this == REMOVE_EVENT || this == BAN_HOST || this == SUSPEND_EVENT;
    }

    /**
     * Check if this action affects the event
     */
    public boolean affectsEvent() {
        return this == REMOVE_EVENT || this == UPDATE_EVENT || this == SUSPEND_EVENT;
    }

    /**
     * Check if this action affects the host
     */
    public boolean affectsHost() {
        return this == WARN_HOST || this == BAN_HOST;
    }
}
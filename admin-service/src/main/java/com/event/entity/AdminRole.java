package com.event.entity;

/**
 * Enum representing the role/level of an admin user.
 * Determines the base level of access and default permissions.
 *
 * @author CityVibe Team
 * @version 1.0
 */
public enum AdminRole {

    /**
     * Super Admin - Full system access, can manage other admins
     * Has all permissions by default
     */
    SUPER_ADMIN("Super Admin", 3),

    /**
     * Admin - Can perform most administrative tasks
     * Cannot manage other admins
     */
    ADMIN("Admin", 2),

    /**
     * Moderator - Limited administrative access
     * Primarily for content moderation
     */
    MODERATOR("Moderator", 1);

    private final String displayName;
    private final int level;

    AdminRole(String displayName, int level) {
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
     * Check if this role is higher than another
     */
    public boolean isHigherThan(AdminRole other) {
        return this.level > other.level;
    }

    /**
     * Check if this role is at least as high as another
     */
    public boolean isAtLeast(AdminRole other) {
        return this.level >= other.level;
    }

    /**
     * Check if this is a super admin
     */
    public boolean isSuperAdmin() {
        return this == SUPER_ADMIN;
    }
}
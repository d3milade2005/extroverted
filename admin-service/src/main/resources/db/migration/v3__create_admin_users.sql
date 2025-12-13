-- =====================================================
-- CityVibe Admin Service - Admin Users Table
-- =====================================================
-- Purpose: Store admin-specific metadata and permissions
-- Note: Authentication handled by Keycloak, this is metadata only
-- =====================================================

CREATE TABLE IF NOT EXISTS admin_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- User Reference (from User Service via Keycloak)
    user_id UUID UNIQUE NOT NULL,

    -- Admin Details
    admin_role VARCHAR(50) NOT NULL,
    department VARCHAR(100),
    permissions TEXT[] NOT NULL DEFAULT '{}',

    -- Status
    is_active BOOLEAN NOT NULL DEFAULT true,

    -- Activity Tracking
    last_login_at TIMESTAMP,
    last_action_at TIMESTAMP,
    total_actions_count INTEGER NOT NULL DEFAULT 0,

    -- Admin Management
    created_by UUID,
    deactivated_by UUID,
    deactivated_at TIMESTAMP,
    deactivation_reason TEXT,

    -- Metadata
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- Comments
-- =====================================================
COMMENT ON TABLE admin_users IS 'Admin-specific metadata and permissions (auth handled by Keycloak)';
COMMENT ON COLUMN admin_users.user_id IS 'Reference to user in User Service / Keycloak';
COMMENT ON COLUMN admin_users.admin_role IS 'Admin role: SUPER_ADMIN, ADMIN, MODERATOR';
COMMENT ON COLUMN admin_users.permissions IS 'Array of permissions: APPROVE_EVENTS, REJECT_EVENTS, VERIFY_PROMOTERS, MANAGE_REPORTS, BAN_USERS, VIEW_ANALYTICS, MANAGE_ADMINS';
COMMENT ON COLUMN admin_users.department IS 'Department: CONTENT_MODERATION, USER_MANAGEMENT, EVENT_MANAGEMENT, GENERAL';

-- =====================================================
-- Indexes for Performance
-- =====================================================
CREATE INDEX idx_admin_users_user_id ON admin_users(user_id);
CREATE INDEX idx_admin_users_role ON admin_users(admin_role);
CREATE INDEX idx_admin_users_active ON admin_users(is_active) WHERE is_active = true;
CREATE INDEX idx_admin_users_last_login ON admin_users(last_login_at DESC) WHERE last_login_at IS NOT NULL;

-- GIN index for permissions array searching
CREATE INDEX idx_admin_users_permissions ON admin_users USING GIN (permissions);

-- =====================================================
-- Check Constraints
-- =====================================================
ALTER TABLE admin_users ADD CONSTRAINT check_admin_role
CHECK (admin_role IN ('SUPER_ADMIN', 'ADMIN', 'MODERATOR'));

ALTER TABLE admin_users ADD CONSTRAINT check_department
CHECK (department IS NULL OR department IN (
    'CONTENT_MODERATION', 'USER_MANAGEMENT',
    'EVENT_MANAGEMENT', 'GENERAL', 'OTHER'
));

-- Ensure deactivated admins have required fields
ALTER TABLE admin_users ADD CONSTRAINT check_deactivation_complete
CHECK (
    (is_active = false AND deactivated_by IS NOT NULL AND deactivated_at IS NOT NULL)
    OR is_active = true
);

-- =====================================================
-- Default Permissions by Role
-- =====================================================
-- Note: These are just suggestions, actual permissions set at creation time
--
-- SUPER_ADMIN: All permissions
-- ADMIN: APPROVE_EVENTS, REJECT_EVENTS, VERIFY_PROMOTERS, MANAGE_REPORTS, BAN_USERS, VIEW_ANALYTICS
-- MODERATOR: APPROVE_EVENTS, REJECT_EVENTS, MANAGE_REPORTS, VIEW_ANALYTICS
-- =====================================================
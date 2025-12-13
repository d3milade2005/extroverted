-- =====================================================
-- CityVibe Admin Service - Admin Actions Audit Trail
-- =====================================================
-- Purpose: Immutable log of all administrative actions
-- Features: Complete audit trail, IP logging, entity tracking
-- =====================================================

CREATE TABLE IF NOT EXISTS admin_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Action Details
    action_type VARCHAR(50) NOT NULL,
    performed_by UUID NOT NULL,
    target_entity_type VARCHAR(50) NOT NULL,
    target_entity_id UUID NOT NULL,

    -- Action Context
    action_description TEXT,
    previous_state JSONB,
    new_state JSONB,
    action_metadata JSONB,

    -- Tracking
    ip_address VARCHAR(45),
    user_agent TEXT,

    -- Timestamp (Immutable - no updated_at)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- Comments
-- =====================================================
COMMENT ON TABLE admin_actions IS 'Immutable audit log of all administrative actions';
COMMENT ON COLUMN admin_actions.action_type IS 'Type of action: APPROVE_EVENT, REJECT_EVENT, APPROVE_PROMOTER, REJECT_PROMOTER, RESOLVE_REPORT, DISMISS_REPORT, BAN_USER, UNBAN_USER, SUSPEND_EVENT, DELETE_EVENT, UPDATE_EVENT, ASSIGN_REPORT, CREATE_ADMIN, UPDATE_ADMIN, DEACTIVATE_ADMIN';
COMMENT ON COLUMN admin_actions.target_entity_type IS 'Type of entity affected: EVENT, USER, PROMOTER, REPORT, ADMIN';
COMMENT ON COLUMN admin_actions.previous_state IS 'State before action (JSON)';
COMMENT ON COLUMN admin_actions.new_state IS 'State after action (JSON)';
COMMENT ON COLUMN admin_actions.action_metadata IS 'Additional context like reason, notes, etc.';

-- =====================================================
-- Indexes for Performance
-- =====================================================
CREATE INDEX idx_admin_actions_performed_by ON admin_actions(performed_by);
CREATE INDEX idx_admin_actions_action_type ON admin_actions(action_type);
CREATE INDEX idx_admin_actions_target ON admin_actions(target_entity_type, target_entity_id);
CREATE INDEX idx_admin_actions_created ON admin_actions(created_at DESC);
CREATE INDEX idx_admin_actions_entity_id ON admin_actions(target_entity_id);

-- Composite index for filtering by admin and date range
CREATE INDEX idx_admin_actions_admin_date ON admin_actions(performed_by, created_at DESC);

-- GIN index for JSONB searching
CREATE INDEX idx_admin_actions_metadata ON admin_actions USING GIN (action_metadata);

-- =====================================================
-- Check Constraints
-- =====================================================
ALTER TABLE admin_actions ADD CONSTRAINT check_action_type
CHECK (action_type IN (
    'APPROVE_EVENT', 'REJECT_EVENT',
    'APPROVE_PROMOTER', 'REJECT_PROMOTER',
    'RESOLVE_REPORT', 'DISMISS_REPORT', 'ASSIGN_REPORT',
    'BAN_USER', 'UNBAN_USER',
    'SUSPEND_EVENT', 'DELETE_EVENT', 'UPDATE_EVENT',
    'CREATE_ADMIN', 'UPDATE_ADMIN', 'DEACTIVATE_ADMIN',
    'OTHER'
));

ALTER TABLE admin_actions ADD CONSTRAINT check_target_entity_type
CHECK (target_entity_type IN (
    'EVENT', 'USER', 'PROMOTER', 'REPORT', 'ADMIN', 'OTHER'
));

-- =====================================================
-- Make Table Immutable (Prevent Updates/Deletes)
-- =====================================================
-- Note: In production, consider using triggers or Row-Level Security
-- to prevent any modifications to this audit table
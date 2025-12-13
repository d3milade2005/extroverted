CREATE TABLE IF NOT EXISTS event_reports (
                                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Report Details
    event_id UUID NOT NULL,
    reported_by UUID NOT NULL,
    report_reason VARCHAR(50) NOT NULL,
    detailed_description TEXT,

    -- Priority & Status
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    -- Assignment & Resolution
    assigned_to UUID,
    assigned_at TIMESTAMP,
    resolved_by UUID,
    resolved_at TIMESTAMP,
    resolution_action VARCHAR(50),
    resolution_notes TEXT,

    -- Additional Information
    evidence_urls TEXT[],
    reporter_ip VARCHAR(45),

    -- Metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- =====================================================
-- Comments
-- =====================================================
COMMENT ON TABLE event_reports IS 'User-submitted reports about potentially problematic events';
COMMENT ON COLUMN event_reports.report_reason IS 'Reason for report: FAKE_EVENT, SCAM_SUSPECTED, INAPPROPRIATE_CONTENT, DUPLICATE_EVENT, WRONG_INFORMATION, CANCELLED_NOT_UPDATED, SAFETY_CONCERNS, SPAM, OTHER';
COMMENT ON COLUMN event_reports.priority IS 'Priority level: LOW, MEDIUM, HIGH, CRITICAL';
COMMENT ON COLUMN event_reports.status IS 'Report status: PENDING, INVESTIGATING, RESOLVED, DISMISSED';
COMMENT ON COLUMN event_reports.resolution_action IS 'Action taken: REMOVE_EVENT, WARN_HOST, UPDATE_EVENT, DISMISS_REPORT, BAN_HOST, SUSPEND_EVENT';
COMMENT ON COLUMN event_reports.evidence_urls IS 'URLs to screenshots or evidence provided by reporter';

-- =====================================================
-- Indexes for Performance
-- =====================================================
CREATE INDEX idx_event_reports_event ON event_reports(event_id);
CREATE INDEX idx_event_reports_status ON event_reports(status);
CREATE INDEX idx_event_reports_priority ON event_reports(priority);
CREATE INDEX idx_event_reports_reported_by ON event_reports(reported_by);
CREATE INDEX idx_event_reports_assigned_to ON event_reports(assigned_to) WHERE assigned_to IS NOT NULL;
CREATE INDEX idx_event_reports_created ON event_reports(created_at DESC);

-- Index for filtering pending high-priority reports
CREATE INDEX idx_event_reports_pending_priority ON event_reports(status, priority, created_at DESC)
    WHERE status = 'PENDING';

-- =====================================================
-- Check Constraints
-- =====================================================
ALTER TABLE event_reports ADD CONSTRAINT check_report_reason
    CHECK (report_reason IN (
                             'FAKE_EVENT', 'SCAM_SUSPECTED', 'INAPPROPRIATE_CONTENT',
                             'DUPLICATE_EVENT', 'WRONG_INFORMATION', 'CANCELLED_NOT_UPDATED',
                             'SAFETY_CONCERNS', 'SPAM', 'OTHER'
        ));

ALTER TABLE event_reports ADD CONSTRAINT check_priority
    CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'));

ALTER TABLE event_reports ADD CONSTRAINT check_status
    CHECK (status IN ('PENDING', 'INVESTIGATING', 'RESOLVED', 'DISMISSED'));

ALTER TABLE event_reports ADD CONSTRAINT check_resolution_action
    CHECK (resolution_action IS NULL OR resolution_action IN (
                                                              'REMOVE_EVENT', 'WARN_HOST', 'UPDATE_EVENT',
                                                              'DISMISS_REPORT', 'BAN_HOST', 'SUSPEND_EVENT'
        ));

-- Ensure resolved reports have resolution info
ALTER TABLE event_reports ADD CONSTRAINT check_resolution_complete
    CHECK (
        (status IN ('RESOLVED', 'DISMISSED') AND resolved_by IS NOT NULL AND resolved_at IS NOT NULL)
            OR status NOT IN ('RESOLVED', 'DISMISSED')
        );
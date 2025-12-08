-- V1__create_recommendation_schema.sql
-- Database schema for Recommendation Service

-- Create recommendation_history table
CREATE TABLE recommendation_history (
                                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                        user_id UUID NOT NULL,
                                        event_id UUID NOT NULL,

    -- Overall score and ranking
                                        score DOUBLE PRECISION NOT NULL CHECK (score >= 0 AND score <= 1),
                                        rank_position INTEGER,

    -- Scoring breakdown (for debugging and analytics)
                                        geo_score DOUBLE PRECISION CHECK (geo_score >= 0 AND geo_score <= 1),
                                        interest_score DOUBLE PRECISION CHECK (interest_score >= 0 AND interest_score <= 1),
                                        interaction_score DOUBLE PRECISION CHECK (interaction_score >= 0 AND interaction_score <= 1),
                                        popularity_score DOUBLE PRECISION CHECK (popularity_score >= 0 AND popularity_score <= 1),
                                        recency_score DOUBLE PRECISION CHECK (recency_score >= 0 AND recency_score <= 1),

    -- User interaction tracking
                                        recommended_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                        clicked BOOLEAN DEFAULT FALSE,
                                        clicked_at TIMESTAMP,
                                        saved BOOLEAN DEFAULT FALSE,
                                        saved_at TIMESTAMP,
                                        converted BOOLEAN DEFAULT FALSE,  -- Did they buy ticket?
                                        converted_at TIMESTAMP,

    -- Metadata
                                        recommendation_reason TEXT[],  -- Array of reasons for recommendation
                                        algorithm_version VARCHAR(10) DEFAULT 'v1.0',
                                        distance_km DOUBLE PRECISION,     -- Distance from user to event

    -- Timestamps
                                        created_at TIMESTAMP DEFAULT NOW(),
                                        updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for query performance
CREATE INDEX idx_rec_user ON recommendation_history(user_id, recommended_at DESC);
CREATE INDEX idx_rec_event ON recommendation_history(event_id);
CREATE INDEX idx_rec_user_event ON recommendation_history(user_id, event_id);
CREATE INDEX idx_rec_clicked ON recommendation_history(clicked) WHERE clicked = TRUE;
CREATE INDEX idx_rec_converted ON recommendation_history(converted) WHERE converted = TRUE;
CREATE INDEX idx_rec_date ON recommendation_history(recommended_at);
CREATE INDEX idx_rec_score ON recommendation_history(score DESC);

-- Trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_recommendation_history_updated_at
    BEFORE UPDATE ON recommendation_history
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Comments for documentation
COMMENT ON TABLE recommendation_history IS 'Stores history of all recommendations shown to users for analytics and ML training';
COMMENT ON COLUMN recommendation_history.score IS 'Overall recommendation score (0.0 to 1.0)';
COMMENT ON COLUMN recommendation_history.geo_score IS 'Component: How close event is to user';
COMMENT ON COLUMN recommendation_history.interest_score IS 'Component: How well event matches user interests';
COMMENT ON COLUMN recommendation_history.interaction_score IS 'Component: Based on past user behavior';
COMMENT ON COLUMN recommendation_history.popularity_score IS 'Component: How trending/popular the event is';
COMMENT ON COLUMN recommendation_history.recency_score IS 'Component: How soon the event is happening';
COMMENT ON COLUMN recommendation_history.clicked IS 'Did user click on this recommendation';
COMMENT ON COLUMN recommendation_history.converted IS 'Did user buy ticket for this event';
COMMENT ON COLUMN recommendation_history.recommendation_reason IS 'Human-readable reasons for the recommendation';
COMMENT ON COLUMN recommendation_history.algorithm_version IS 'Version of recommendation algorithm used';
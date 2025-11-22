-- Enable PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;

-- Create event categories table
CREATE TABLE categories (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            name VARCHAR(100) NOT NULL UNIQUE,
                            description TEXT,
                            icon VARCHAR(255),
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create events table
CREATE TABLE events (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        title VARCHAR(255) NOT NULL,
                        description TEXT NOT NULL,
                        category_id UUID NOT NULL REFERENCES categories(id),
                        host_id UUID NOT NULL, -- Reference to user service
                        venue VARCHAR(255) NOT NULL,
                        address TEXT NOT NULL,
                        location GEOGRAPHY(Point, 4326) NOT NULL, -- PostGIS geographic point
                        start_time TIMESTAMP NOT NULL,
                        end_time TIMESTAMP NOT NULL,
                        image_url TEXT,
                        ticket_price NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
                        ticket_limit INTEGER,
                        tickets_sold INTEGER NOT NULL DEFAULT 0,
                        verified BOOLEAN NOT NULL DEFAULT false,
                        status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, CANCELLED, COMPLETED
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT check_end_after_start CHECK (end_time > start_time),
                        CONSTRAINT check_ticket_price CHECK (ticket_price >= 0),
                        CONSTRAINT check_ticket_limit CHECK (ticket_limit IS NULL OR ticket_limit > 0),
                        CONSTRAINT check_tickets_sold CHECK (tickets_sold >= 0)
);

-- Create interactions table (for recommendations)
CREATE TABLE interactions (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              user_id UUID NOT NULL, -- Reference to user service
                              event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
                              type VARCHAR(20) NOT NULL, -- VIEW, SAVE, SHARE, RSVP, BUY
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              CONSTRAINT unique_user_event_type UNIQUE (user_id, event_id, type)
);

-- Create indexes for performance
CREATE INDEX idx_events_category ON events(category_id);
CREATE INDEX idx_events_host ON events(host_id);
CREATE INDEX idx_events_status ON events(status);
CREATE INDEX idx_events_start_time ON events(start_time);
CREATE INDEX idx_events_verified ON events(verified);

-- PostGIS spatial index for location-based queries
CREATE INDEX idx_events_location ON events USING GIST(location);

-- Indexes for interactions
CREATE INDEX idx_interactions_user ON interactions(user_id);
CREATE INDEX idx_interactions_event ON interactions(event_id);
CREATE INDEX idx_interactions_type ON interactions(type);
CREATE INDEX idx_interactions_created_at ON interactions(created_at);

-- Insert default categories
INSERT INTO categories (name, description, icon) VALUES
     ('music', 'Music concerts, festivals, and live performances', 'music'),
     ('tech', 'Technology meetups, conferences, and hackathons', 'computer'),
     ('fashion', 'Fashion shows, exhibitions, and style events', 'tshirt'),
     ('food', 'Food festivals, tastings, and culinary experiences', 'utensils'),
     ('nightlife', 'Clubs, parties, and nighttime entertainment', 'moon'),
     ('sports', 'Sports events, matches, and fitness activities', 'dumbbell'),
     ('art', 'Art exhibitions, galleries, and cultural events', 'palette'),
     ('business', 'Business networking, conferences, and seminars', 'briefcase'),
     ('education', 'Workshops, classes, and learning sessions', 'book'),
     ('wellness', 'Health, wellness, and mindfulness events', 'heart');

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers for updated_at
CREATE TRIGGER update_events_updated_at
    BEFORE UPDATE ON events
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_categories_updated_at
    BEFORE UPDATE ON categories
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
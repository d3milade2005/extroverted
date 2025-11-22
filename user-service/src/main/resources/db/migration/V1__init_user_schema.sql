-- Enable PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;

-- Create users table
CREATE TABLE users (
                       keycloak_id VARCHAR(255) PRIMARY KEY,
                       username VARCHAR(255) NOT NULL UNIQUE,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       first_name VARCHAR(255),
                       last_name VARCHAR(255),
                       city VARCHAR(255),
                       location GEOGRAPHY(Point, 4326),
                       fashion_style JSONB,
                       interests TEXT[],
                       role VARCHAR(50) NOT NULL DEFAULT 'USER',
                       verified BOOLEAN NOT NULL DEFAULT FALSE,
                       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Create indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_city ON users(city);
CREATE INDEX idx_users_role ON users(role);

-- Create spatial index for location queries (PostGIS)
CREATE INDEX idx_users_location ON users USING GIST(location);

-- Create GIN index for JSONB fashion_style
CREATE INDEX idx_users_fashion_style ON users USING GIN(fashion_style);

-- Create GIN index for TEXT[] interests
CREATE INDEX idx_users_interests ON users USING GIN(interests);

-- Create trigger for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
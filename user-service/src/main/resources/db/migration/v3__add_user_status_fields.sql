ALTER TABLE users ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_users_active ON users(is_active);
CREATE INDEX idx_users_verified ON users(verified);
CREATE INDEX idx_users_role ON users(role);
-- V20: Add default role and center to users table for auto-assignment feature

ALTER TABLE users
ADD COLUMN default_role_id INT NULL,
ADD COLUMN default_center_id INT NULL;

-- Add foreign key constraints
ALTER TABLE users
ADD CONSTRAINT fk_users_default_role
FOREIGN KEY (default_role_id) REFERENCES roles(role_id);

ALTER TABLE users
ADD CONSTRAINT fk_users_default_center
FOREIGN KEY (default_center_id) REFERENCES centers(center_id);

-- Add indexes for performance
CREATE INDEX idx_users_default_role ON users(default_role_id);
CREATE INDEX idx_users_default_center ON users(default_center_id);
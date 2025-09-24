-- V6: Remove is_active columns

-- Centers: drop column
ALTER TABLE centers
DROP COLUMN is_active;

-- User roles: drop column
ALTER TABLE user_roles
DROP COLUMN is_active;

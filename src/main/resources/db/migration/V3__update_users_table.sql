ALTER TABLE users
DROP COLUMN password_hash,
    ADD COLUMN keycloak_user_id VARCHAR(64) NOT NULL UNIQUE AFTER phone;

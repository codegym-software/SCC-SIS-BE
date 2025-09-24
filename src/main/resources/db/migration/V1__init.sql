-- V1: init schema (idempotent). Viết để có thể chạy OUT OF ORDER mà không phá DB hiện có.

CREATE TABLE IF NOT EXISTS centers (
                                       center_id INT AUTO_INCREMENT PRIMARY KEY,
                                       code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME,
    updated_at DATETIME
    );

CREATE TABLE IF NOT EXISTS users (
                                     user_id INT AUTO_INCREMENT PRIMARY KEY,
                                     full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(32) NOT NULL,
    dob DATE,
    gender VARCHAR(16),
    national_id_no VARCHAR(64),
    start_date DATE,
    specialty VARCHAR(255),
    experience TEXT,
    address_line VARCHAR(255),
    province VARCHAR(128),
    district VARCHAR(128),
    ward VARCHAR(128),
    education_level VARCHAR(128),
    note TEXT,
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at DATETIME,
    created_at DATETIME,
    updated_at DATETIME,
    deleted_at DATETIME,
    INDEX idx_users_full_name (full_name),
    INDEX idx_users_phone (phone)
    );

CREATE TABLE IF NOT EXISTS roles (
                                     role_id INT AUTO_INCREMENT PRIMARY KEY,
                                     name VARCHAR(128) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME,
    updated_at DATETIME
    );

CREATE TABLE IF NOT EXISTS user_roles (
                                          user_role_id INT AUTO_INCREMENT PRIMARY KEY,
                                          user_id INT NOT NULL,
                                          role_id INT NOT NULL,
                                          center_id INT NOT NULL,
                                          is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                          assigned_at DATETIME,
                                          revoked_at DATETIME,
                                          created_at DATETIME,
                                          CONSTRAINT uk_user_roles UNIQUE (user_id, role_id, center_id),
    INDEX idx_user_roles_user (user_id),
    INDEX idx_user_roles_center (center_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(role_id),
    CONSTRAINT fk_user_roles_center FOREIGN KEY (center_id) REFERENCES centers(center_id)
    );

CREATE TABLE IF NOT EXISTS password_reset_tokens (
                                                     id INT AUTO_INCREMENT PRIMARY KEY,
                                                     user_id INT NOT NULL,
                                                     code_hash VARCHAR(255) NOT NULL,
    token VARCHAR(255) UNIQUE,
    expires_at DATETIME NOT NULL,
    used_at DATETIME,
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    resend_count INT NOT NULL DEFAULT 0,
    last_sent_at DATETIME,
    locked_at DATETIME,
    created_at DATETIME,
    request_ip VARCHAR(64),
    user_agent VARCHAR(512),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users(user_id)
    );

CREATE TABLE IF NOT EXISTS auth_tokens (
                                           auth_token_id INT AUTO_INCREMENT PRIMARY KEY,
                                           user_id INT NOT NULL,
                                           token_hash VARCHAR(255) NOT NULL UNIQUE,
    issued_at DATETIME,
    expires_at DATETIME,
    revoked_at DATETIME,
    user_agent VARCHAR(512),
    ip_address VARCHAR(64),
    CONSTRAINT fk_auth_tokens_user FOREIGN KEY (user_id) REFERENCES users(user_id)
    );

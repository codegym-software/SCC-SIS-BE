-- V10: Tạo bảng permissions và role_permissions

-- Tạo bảng permissions
CREATE TABLE permissions (
    permission_id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE COMMENT 'Mã quyền duy nhất (VD: USER_READ, USER_WRITE, CENTER_MANAGE)',
    name VARCHAR(255) NOT NULL COMMENT 'Tên hiển thị của quyền',
    description TEXT COMMENT 'Mô tả chi tiết về quyền',
    category VARCHAR(100) COMMENT 'Nhóm quyền (VD: USER, CENTER, ROLE)',
    active BOOLEAN DEFAULT TRUE COMMENT 'Quyền có đang hoạt động',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes
    INDEX idx_permissions_code (code),
    INDEX idx_permissions_category (category),
    INDEX idx_permissions_active (active)
);

-- Tạo bảng role_permissions (Many-to-Many giữa roles và permissions)
CREATE TABLE role_permissions (
    role_permission_id INT AUTO_INCREMENT PRIMARY KEY,
    role_id INT NOT NULL,
    permission_id INT NOT NULL,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm cấp quyền',
    granted_by VARCHAR(255) COMMENT 'Người cấp quyền (Keycloak ID)',
    
    -- Foreign keys
    CONSTRAINT fk_role_permissions_role 
        FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission 
        FOREIGN KEY (permission_id) REFERENCES permissions(permission_id) ON DELETE CASCADE,
    
    -- Unique constraint: một role chỉ có một lần được gán một permission
    CONSTRAINT uk_role_permissions_role_permission UNIQUE (role_id, permission_id),
    
    -- Indexes
    INDEX idx_role_permissions_role (role_id),
    INDEX idx_role_permissions_permission (permission_id)
);

-- Seed dữ liệu mẫu cho permissions
INSERT INTO permissions (code, name, description, category, active) VALUES
-- User management permissions
('USER_READ', 'Xem danh sách người dùng', 'Quyền xem thông tin và danh sách người dùng', 'USER', TRUE),
('USER_CREATE', 'Tạo người dùng mới', 'Quyền tạo tài khoản người dùng mới', 'USER', TRUE),
('USER_UPDATE', 'Cập nhật thông tin người dùng', 'Quyền chỉnh sửa thông tin người dùng', 'USER', TRUE),
('USER_DELETE', 'Xóa người dùng', 'Quyền xóa tài khoản người dùng', 'USER', TRUE),

-- Center management permissions
('CENTER_READ', 'Xem thông tin trung tâm', 'Quyền xem thông tin và danh sách trung tâm', 'CENTER', TRUE),
('CENTER_CREATE', 'Tạo trung tâm mới', 'Quyền tạo trung tâm mới', 'CENTER', TRUE),
('CENTER_UPDATE', 'Cập nhật thông tin trung tâm', 'Quyền chỉnh sửa thông tin trung tâm', 'CENTER', TRUE),
('CENTER_DELETE', 'Xóa trung tâm', 'Quyền xóa trung tâm', 'CENTER', TRUE),
('CENTER_MANAGE', 'Quản lý trung tâm', 'Quyền quản lý toàn bộ hoạt động của trung tâm', 'CENTER', TRUE),

-- Role management permissions
('ROLE_READ', 'Xem danh sách vai trò', 'Quyền xem thông tin và danh sách vai trò', 'ROLE', TRUE),
('ROLE_CREATE', 'Tạo vai trò mới', 'Quyền tạo vai trò mới trong hệ thống', 'ROLE', TRUE),
('ROLE_UPDATE', 'Cập nhật vai trò', 'Quyền chỉnh sửa thông tin vai trò', 'ROLE', TRUE),
('ROLE_DELETE', 'Xóa vai trò', 'Quyền xóa vai trò khỏi hệ thống', 'ROLE', TRUE),
('ROLE_ASSIGN', 'Gán vai trò cho người dùng', 'Quyền gán vai trò cho người dùng tại trung tâm', 'ROLE', TRUE),
('ROLE_REVOKE', 'Thu hồi vai trò từ người dùng', 'Quyền thu hồi vai trò từ người dùng', 'ROLE', TRUE),

-- Permission management permissions
('PERMISSION_READ', 'Xem danh sách quyền', 'Quyền xem thông tin và danh sách quyền', 'PERMISSION', TRUE),
('PERMISSION_ASSIGN', 'Gán quyền cho vai trò', 'Quyền gán quyền cụ thể cho vai trò', 'PERMISSION', TRUE),
('PERMISSION_REVOKE', 'Thu hồi quyền từ vai trò', 'Quyền thu hồi quyền từ vai trò', 'PERMISSION', TRUE),

-- System administration permissions
('SYSTEM_ADMIN', 'Quản trị hệ thống', 'Quyền quản trị toàn bộ hệ thống', 'SYSTEM', TRUE),
('SYSTEM_CONFIG', 'Cấu hình hệ thống', 'Quyền cấu hình các thiết lập hệ thống', 'SYSTEM', TRUE);
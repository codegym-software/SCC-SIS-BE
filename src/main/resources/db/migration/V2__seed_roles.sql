-- Seed dữ liệu bảng roles
INSERT INTO roles (name, is_active, created_at)
VALUES
    ('Super Admin', 1, NOW()),
    ('Giáo vụ', 1, NOW()),
    ('Giảng viên', 1, NOW()),
    ('Quản lý Trung tâm', 1, NOW()),
    ('Quản lý Đào tạo', 1, NOW());

-- Migration: Thêm vai trò STUDENT vào hệ thống

-- Thêm role STUDENT nếu chưa tồn tại
INSERT INTO roles (code, name, scope, active, created_at, updated_at)
SELECT 'STUDENT', 'Học viên', 'CENTER', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'STUDENT');


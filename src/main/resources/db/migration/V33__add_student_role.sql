-- V33__add_student_role.sql
-- Thêm role STUDENT cho học viên
-- Role này sẽ được gán tự động khi tạo học viên mới

-- Kiểm tra xem đã có role STUDENT chưa, nếu chưa thì insert
INSERT INTO roles (code, name, is_active, created_at, updated_at)
SELECT 'STUDENT', 'Học viên', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'STUDENT');

-- Lưu ý: role STUDENT là GLOBAL role (không cần center_id)


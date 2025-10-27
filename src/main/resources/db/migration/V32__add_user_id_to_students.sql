-- V32__add_user_id_to_students.sql
-- Thêm cột user_id vào bảng students để liên kết với tài khoản đăng nhập
-- Mục tiêu: Khi tạo học viên mới, tự động tạo tài khoản User + Keycloak

ALTER TABLE students
    ADD COLUMN user_id INT NULL COMMENT 'FK -> users.user_id, tài khoản đăng nhập của học viên';

-- Thêm foreign key constraint
ALTER TABLE students
    ADD CONSTRAINT fk_students_user 
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL;

-- Thêm index cho performance
CREATE INDEX idx_students_user_id ON students (user_id) COMMENT 'Index cho tìm kiếm theo user_id';

-- Thêm unique constraint để đảm bảo 1 user chỉ link với 1 student
ALTER TABLE students
    ADD CONSTRAINT uk_students_user_id UNIQUE (user_id);


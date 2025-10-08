-- V16__create_students_table.sql
-- Bảng lưu thông tin học viên (Student)
-- + Chuẩn hóa ENUM UPPERCASE cho đồng bộ với Java enum
-- + Chuyển dữ liệu gender trong bảng users sang UPPERCASE để đồng bộ hệ thống

CREATE TABLE students (
                          student_id INT AUTO_INCREMENT PRIMARY KEY,

                          full_name       VARCHAR(255) NOT NULL COMMENT 'Họ và tên học viên',
                          email           VARCHAR(255) NOT NULL COMMENT 'Email học viên',
                          phone           VARCHAR(32)  NOT NULL COMMENT 'Số điện thoại học viên',
                          dob             DATE NULL COMMENT 'Ngày sinh',

    -- Sử dụng ENUM UPPERCASE để đồng bộ với enum GenderType { MALE, FEMALE, OTHER }
                          gender ENUM('MALE', 'FEMALE', 'OTHER') NULL COMMENT 'Giới tính học viên',

                          national_id_no  VARCHAR(64)  NULL COMMENT 'Số CMND/CCCD hoặc hộ chiếu',
                          address_line    VARCHAR(255) NULL COMMENT 'Địa chỉ chi tiết',
                          province        VARCHAR(128) NULL COMMENT 'Tỉnh/Thành phố',
                          district        VARCHAR(128) NULL COMMENT 'Quận/Huyện',
                          ward            VARCHAR(128) NULL COMMENT 'Phường/Xã',

    -- Trạng thái hồ sơ học viên (đồng bộ enum OverallStatus { ACTIVE, INACTIVE, GRADUATED, SUSPENDED })
                          overall_status ENUM('ACTIVE', 'INACTIVE', 'GRADUATED', 'SUSPENDED') NULL DEFAULT 'ACTIVE'
                              COMMENT 'Trạng thái tổng quát của học viên',

                          note        TEXT NULL COMMENT 'Ghi chú bổ sung',

                          created_by  INT NULL COMMENT 'FK -> users.user_id, người tạo bản ghi',
                          updated_by  INT NULL COMMENT 'FK -> users.user_id, người cập nhật bản ghi',
                          deleted_at  DATETIME NULL COMMENT 'Thời điểm xóa mềm',
                          created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm tạo',
                          updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời điểm cập nhật',

    -- Ràng buộc
                          CONSTRAINT uk_students_email UNIQUE (email),
    -- Khóa ngoại đến người tạo
                          CONSTRAINT fk_students_created_by FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL,
    -- Khóa ngoại đến người cập nhật
                          CONSTRAINT fk_students_updated_by FOREIGN KEY (updated_by) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ===== Indexes =====
CREATE INDEX idx_students_email ON students (email) COMMENT 'Chỉ mục cho tìm kiếm email';
CREATE INDEX idx_students_full_name ON students (full_name) COMMENT 'Chỉ mục cho tìm kiếm tên';
CREATE INDEX idx_students_overall_status ON students (overall_status) COMMENT 'Chỉ mục cho lọc trạng thái';

-- ===================================================================
-- BƯỚC BỔ SUNG: CHUẨN HOÁ DỮ LIỆU BẢNG USERS (gender -> UPPERCASE)
-- ===================================================================
-- Mục tiêu: đồng bộ với enum GenderType { MALE, FEMALE, OTHER } trong Java
-- Lưu ý: câu lệnh này an toàn, chỉ tác động nếu giá trị hiện tại là chữ thường
UPDATE users
SET gender = UPPER(gender)
WHERE gender IN ('male', 'female', 'other');

-- V17__create_enrollments_table.sql
-- Bảng ghi danh học viên vào lớp (Enrollment)
-- + Có revoked_at để hỗ trợ xóa mềm (hủy ghi danh)
-- + Có cột sinh eff_end_date để filter hiệu lực
-- + Index tối ưu cho truy vấn list / active / filter theo lớp

CREATE TABLE enrollments (
                             enrollment_id INT AUTO_INCREMENT PRIMARY KEY,

                             class_id   INT NOT NULL COMMENT 'FK -> classes.class_id, mã lớp học',
                             student_id INT NOT NULL COMMENT 'FK -> students.student_id, mã học viên',

                             status ENUM('ACTIVE', 'DROPPED', 'SUSPENDED') NOT NULL DEFAULT 'ACTIVE' COMMENT 'Trạng thái ghi danh',
                             enrolled_at DATE NOT NULL COMMENT 'Ngày bắt đầu ghi danh',
                             left_at     DATE NULL  COMMENT 'Ngày kết thúc ghi danh (NULL = còn hiệu lực)',

                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm tạo',
                             updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời điểm cập nhật',

                             assigned_by INT NULL COMMENT 'Người gán học viên vào lớp',
                             revoked_by  INT NULL COMMENT 'Người hủy ghi danh',
                             revoked_at  DATETIME NULL COMMENT 'Thời điểm hủy ghi danh (xóa mềm)',
                             note        TEXT NULL COMMENT 'Ghi chú bổ sung',

    -- Ràng buộc dữ liệu
                             CONSTRAINT chk_enroll_dates_order CHECK (left_at IS NULL OR enrolled_at <= left_at),
                             CONSTRAINT uk_enrollments_class_student_enrolled UNIQUE (class_id, student_id, enrolled_at),

    -- Khóa ngoại
                             CONSTRAINT fk_enrollments_class
                                 FOREIGN KEY (class_id) REFERENCES classes(class_id) ON DELETE CASCADE,
                             CONSTRAINT fk_enrollments_student
                                 FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE RESTRICT,
                             CONSTRAINT fk_enrollments_assigned_by
                                 FOREIGN KEY (assigned_by) REFERENCES users(user_id) ON DELETE SET NULL,
                             CONSTRAINT fk_enrollments_revoked_by
                                 FOREIGN KEY (revoked_by) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ===== Generated column: hiệu lực =====
ALTER TABLE enrollments
    ADD COLUMN eff_end_date DATE
    AS (COALESCE(left_at, DATE '9999-12-31')) VIRTUAL
    COMMENT 'Cột sinh: phục vụ lọc nhanh bản ghi còn hiệu lực';

-- ===== Indexes =====
CREATE INDEX idx_enrollments_class_effend_start_id
    ON enrollments (class_id, eff_end_date, enrolled_at, enrollment_id)
    COMMENT 'Lọc danh sách hiệu lực theo lớp';

CREATE INDEX idx_enrollments_class_status
    ON enrollments (class_id, status)
    COMMENT 'Lọc trạng thái theo lớp';

CREATE INDEX idx_enrollments_student_start_id
    ON enrollments (student_id, enrolled_at, enrollment_id)
    COMMENT 'Tra cứu theo học viên';

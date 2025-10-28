-- V34__create_attendance_tables.sql
-- Bảng quản lý điểm danh học viên

-- ===== Bảng 1: attendance_sessions (Đợt điểm danh) =====
CREATE TABLE attendance_sessions (
    session_id INT AUTO_INCREMENT PRIMARY KEY,
   
    class_id INT NOT NULL COMMENT 'FK -> classes.class_id, lớp học',
    teacher_id INT NOT NULL COMMENT 'FK -> users.user_id, giảng viên điểm danh',
   
    attendance_date DATE NOT NULL COMMENT 'Ngày điểm danh',
    notes TEXT NULL COMMENT 'Ghi chú của buổi điểm danh',
   
    total_students INT NOT NULL DEFAULT 0 COMMENT 'Tổng số học viên',
    present_count INT NOT NULL DEFAULT 0 COMMENT 'Số học viên có mặt',
    absent_count INT NOT NULL DEFAULT 0 COMMENT 'Số học viên vắng mặt',
   
    created_by INT NULL COMMENT 'Người tạo đợt điểm danh',
    updated_by INT NULL COMMENT 'Người cập nhật đợt điểm danh',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm tạo',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời điểm cập nhật',
    deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Đánh dấu xóa mềm',
    deleted_at DATETIME NULL COMMENT 'Thời điểm xóa',
    deleted_by INT NULL COMMENT 'Người xóa đợt điểm danh',
   
    -- Khóa ngoại
    CONSTRAINT fk_attendance_sessions_class
        FOREIGN KEY (class_id) REFERENCES classes(class_id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_sessions_teacher
        FOREIGN KEY (teacher_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_attendance_sessions_created_by
        FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL,
    CONSTRAINT fk_attendance_sessions_updated_by
        FOREIGN KEY (updated_by) REFERENCES users(user_id) ON DELETE SET NULL,
    CONSTRAINT fk_attendance_sessions_deleted_by
        FOREIGN KEY (deleted_by) REFERENCES users(user_id) ON DELETE SET NULL,
   
    -- Unique constraint: mỗi lớp chỉ có 1 đợt điểm danh cho 1 ngày
    CONSTRAINT uk_attendance_sessions_class_date
        UNIQUE (class_id, attendance_date)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ===== Bảng 2: attendance_records (Chi tiết điểm danh từng học viên) =====
CREATE TABLE attendance_records (
    record_id INT AUTO_INCREMENT PRIMARY KEY,
   
    session_id INT NOT NULL COMMENT 'FK -> attendance_sessions.session_id, đợt điểm danh',
    enrollment_id INT NOT NULL COMMENT 'FK -> enrollments.enrollment_id, mã ghi danh',
    student_id INT NOT NULL COMMENT 'FK -> students.student_id, mã học viên',
   
    status ENUM('PRESENT', 'ABSENT') NOT NULL COMMENT 'Trạng thái: có mặt / vắng mặt',
    notes TEXT NULL COMMENT 'Ghi chú cho học viên này',
   
    created_by INT NULL COMMENT 'Người tạo bản ghi',
    updated_by INT NULL COMMENT 'Người cập nhật bản ghi',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm tạo',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời điểm cập nhật',
    deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Đánh dấu xóa mềm',
    deleted_at DATETIME NULL COMMENT 'Thời điểm xóa',
    deleted_by INT NULL COMMENT 'Người xóa bản ghi',
   
    -- Khóa ngoại
    CONSTRAINT fk_attendance_records_session
        FOREIGN KEY (session_id) REFERENCES attendance_sessions(session_id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_records_enrollment
        FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id) ON DELETE RESTRICT,
    CONSTRAINT fk_attendance_records_student
        FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE RESTRICT,
    CONSTRAINT fk_attendance_records_created_by
        FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL,
    CONSTRAINT fk_attendance_records_updated_by
        FOREIGN KEY (updated_by) REFERENCES users(user_id) ON DELETE SET NULL,
    CONSTRAINT fk_attendance_records_deleted_by
        FOREIGN KEY (deleted_by) REFERENCES users(user_id) ON DELETE SET NULL,
   
    -- Unique constraint: mỗi học viên chỉ có 1 bản ghi trong 1 đợt điểm danh
    CONSTRAINT uk_attendance_records_session_student
        UNIQUE (session_id, student_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ===== Indexes để tối ưu truy vấn =====
-- Tìm đợt điểm danh theo lớp và ngày
CREATE INDEX idx_attendance_sessions_class_date
    ON attendance_sessions (class_id, attendance_date DESC, session_id DESC)
    COMMENT 'Tìm đợt điểm danh theo lớp và ngày';

-- Tìm đợt điểm danh theo giảng viên và ngày
CREATE INDEX idx_attendance_sessions_teacher_date
    ON attendance_sessions (teacher_id, attendance_date DESC, session_id DESC)
    COMMENT 'Tìm đợt điểm danh theo giảng viên và ngày';

-- Tìm bản ghi điểm danh theo đợt điểm danh
CREATE INDEX idx_attendance_records_session
    ON attendance_records (session_id)
    COMMENT 'Tìm bản ghi điểm danh theo đợt điểm danh';

-- Tìm bản ghi điểm danh theo học viên
CREATE INDEX idx_attendance_records_student
    ON attendance_records (student_id, created_at DESC)
    COMMENT 'Tra cứu điểm danh của học viên';

-- Tìm bản ghi điểm danh theo trạng thái
CREATE INDEX idx_attendance_records_status
    ON attendance_records (session_id, status)
    COMMENT 'Lọc theo trạng thái có mặt/vắng mặt';


-- V14__create_class_teachers_table.sql
-- Class–Teacher assignment (timeline) + audit + deterministic optimizations

CREATE TABLE class_teachers (
                                class_teacher_id INT AUTO_INCREMENT PRIMARY KEY,

                                class_id   INT NOT NULL COMMENT 'FK -> classes.class_id',
                                teacher_id INT NOT NULL COMMENT 'FK -> users.user_id',

    -- Timeline
                                start_date DATE NOT NULL COMMENT 'Ngày bắt đầu phụ trách',
                                end_date   DATE NULL COMMENT 'Ngày kết thúc (NULL = còn hiệu lực)',

    -- Audit
                                created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                assigned_by INT NULL COMMENT 'User thực hiện gán giảng viên',
                                revoked_by  INT NULL COMMENT 'User kết thúc phân công',
                                note        TEXT NULL COMMENT 'Ghi chú nghiệp vụ (tuỳ chọn)',

    -- Ràng buộc dữ liệu
                                CONSTRAINT chk_ct_dates_order CHECK (end_date IS NULL OR start_date <= end_date),

    -- Cho phép nhiều giai đoạn cho cùng (class,teacher) nhưng cấm TRÙNG start_date
                                CONSTRAINT uk_ct_class_teacher_start UNIQUE (class_id, teacher_id, start_date),

    -- Khóa ngoại
                                CONSTRAINT fk_class_teachers_class
                                    FOREIGN KEY (class_id)  REFERENCES classes(class_id) ON DELETE CASCADE,
                                CONSTRAINT fk_class_teachers_teacher
                                    FOREIGN KEY (teacher_id) REFERENCES users(user_id)    ON DELETE RESTRICT,
                                CONSTRAINT fk_ct_assigned_by
                                    FOREIGN KEY (assigned_by) REFERENCES users(user_id)   ON DELETE SET NULL,
                                CONSTRAINT fk_ct_revoked_by
                                    FOREIGN KEY (revoked_by)  REFERENCES users(user_id)   ON DELETE SET NULL
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- Generated column để filter "đang hiệu lực" nhanh
ALTER TABLE class_teachers
    ADD COLUMN eff_end_date DATE
    AS (COALESCE(end_date, '9999-12-31')) VIRTUAL COMMENT 'Dùng để filter “đang hiệu lực” nhanh (index-friendly)';

-- ===== Indexes
-- 1) Active list theo lớp: WHERE class_id=? AND eff_end_date>=CURDATE() ORDER BY start_date
CREATE INDEX idx_ct_class_effend_start_id
    ON class_teachers (class_id, eff_end_date, start_date, class_teacher_id);

-- 2) Timeline theo lớp
CREATE INDEX idx_ct_class_start_id
    ON class_teachers (class_id, start_date, class_teacher_id);

-- 3) Tra cứu theo giảng viên
CREATE INDEX idx_ct_teacher_start_id
    ON class_teachers (teacher_id, start_date, class_teacher_id);

-- 4) (Khuyến nghị) Phục vụ trigger overlap: tra theo (class_id, teacher_id)
CREATE INDEX idx_ct_class_teacher_range
    ON class_teachers (class_id, teacher_id, start_date, end_date);
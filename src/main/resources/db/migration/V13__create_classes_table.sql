-- V13__create_classes_table.sql
CREATE TABLE classes (
                         class_id      INT AUTO_INCREMENT PRIMARY KEY,
                         center_id     INT NOT NULL COMMENT 'Trung tâm tổ chức lớp học',
                         program_id    INT NOT NULL COMMENT 'Chương trình học',

                         name          VARCHAR(255) NOT NULL COMMENT 'Tên lớp học',
                         description   TEXT NULL COMMENT 'Mô tả lớp học',

                         start_date    DATE NULL COMMENT 'Ngày khai giảng',
                         end_date      DATE NULL COMMENT 'Ngày kết thúc',

    -- Trạng thái vận hành
                         status        ENUM('PLANNED','ONGOING','FINISHED','CANCELLED')
                             NOT NULL DEFAULT 'PLANNED' COMMENT 'Trạng thái lớp',

    -- Thuộc tính vận hành (tùy nhu cầu)
                         room          VARCHAR(100) NULL COMMENT 'Phòng học',
                         capacity      INT UNSIGNED NULL COMMENT 'Sĩ số tối đa',

    -- Soft delete + audit
                         deleted_at    DATETIME DEFAULT NULL COMMENT 'Thời điểm soft delete',
                         created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         created_by    INT DEFAULT NULL COMMENT 'ID user tạo lớp học',
                         updated_by    INT DEFAULT NULL COMMENT 'ID user cập nhật cuối',

    -- Ràng buộc dữ liệu
                         CONSTRAINT chk_class_dates_order CHECK (
                             start_date IS NULL OR end_date IS NULL OR start_date <= end_date
                             ),
                         CONSTRAINT chk_class_capacity_positive CHECK (
                             capacity IS NULL OR capacity > 0
                             ),

    -- UNIQUE: tên lớp không trùng trong cùng một trung tâm
                         CONSTRAINT uk_classes_center_name UNIQUE (center_id, name),

    -- Khóa ngoại
                         CONSTRAINT fk_classes_center
                             FOREIGN KEY (center_id) REFERENCES centers(center_id) ON DELETE RESTRICT,
                         CONSTRAINT fk_classes_program
                             FOREIGN KEY (program_id) REFERENCES programs(program_id) ON DELETE RESTRICT,
                         CONSTRAINT fk_classes_created_by
                             FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL,
                         CONSTRAINT fk_classes_updated_by
                             FOREIGN KEY (updated_by) REFERENCES users(user_id) ON DELETE SET NULL
);

-- Indexes tối ưu cho list/filter phân trang “seek-friendly”
-- 1) List theo center + status + sort theo start_date (kèm class_id làm tie-breaker)
CREATE INDEX idx_classes_center_status_start_id
    ON classes (center_id, status, start_date, class_id);

-- 2) Lọc theo chương trình
CREATE INDEX idx_classes_program ON classes (program_id);

-- Tuỳ chọn theo nhu cầu thực tế:
-- CREATE FULLTEXT INDEX ftx_classes_name_desc ON classes (name, description);
-- CREATE INDEX idx_classes_center_status_end_id ON classes (center_id, status, end_date, class_id);
-- CREATE INDEX idx_classes_status_start ON classes (status, start_date);
-- CREATE INDEX idx_classes_deleted_at_only ON classes (deleted_at);

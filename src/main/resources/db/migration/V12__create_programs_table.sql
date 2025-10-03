-- V12__create_programs_table.sql
CREATE TABLE programs (
                          program_id      INT AUTO_INCREMENT PRIMARY KEY,

    -- Định danh & mô tả
                          code            VARCHAR(50)  NOT NULL UNIQUE COMMENT 'Mã chương trình (VD: JAVA_BASIC, REACTJS_ADV)',
                          name            VARCHAR(255) NOT NULL COMMENT 'Tên chương trình học',
                          description     TEXT COMMENT 'Mô tả chi tiết chương trình',

    -- Thuộc tính phục vụ tạo lớp & hiển thị
                          duration_hours  INT UNSIGNED DEFAULT NULL COMMENT 'Tổng thời lượng (giờ)',
                          delivery_mode   ENUM('OFFLINE','ONLINE','HYBRID') NOT NULL DEFAULT 'OFFLINE' COMMENT 'Hình thức học',
                          category_code   VARCHAR(50)  DEFAULT NULL COMMENT 'Nhóm/chuyên mục (VD: IELTS, TOEIC, Kids...)',
                          level           VARCHAR(50)  DEFAULT NULL COMMENT 'Trình độ (VD: Beginner/Intermediate/Advanced)',
                          language_code   VARCHAR(10)  DEFAULT NULL COMMENT 'Ngôn ngữ giảng dạy (vi/en)',

    -- Trạng thái & vòng đời
                          is_active       BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Đang hoạt động để chọn mở lớp',
                          deleted_at      DATETIME DEFAULT NULL COMMENT 'Soft delete: ẩn hẳn nhưng giữ lịch sử',

    -- Audit
                          created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          created_by      INT DEFAULT NULL COMMENT 'ID user tạo chương trình',
                          updated_by      INT DEFAULT NULL COMMENT 'ID user cập nhật cuối',

    -- Ràng buộc dữ liệu
                          CONSTRAINT chk_program_duration_positive CHECK (duration_hours IS NULL OR duration_hours > 0),
                          CONSTRAINT chk_program_deleted_implies_inactive CHECK (deleted_at IS NULL OR is_active = FALSE),

    -- Khóa ngoại (audit)
                          CONSTRAINT fk_programs_created_by
                              FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL,
                          CONSTRAINT fk_programs_updated_by
                              FOREIGN KEY (updated_by) REFERENCES users(user_id) ON DELETE SET NULL
);

-- Indexes phục vụ lọc/tìm kiếm
CREATE INDEX idx_programs_is_active   ON programs (is_active);
CREATE INDEX idx_programs_deleted_at  ON programs (deleted_at);
CREATE INDEX idx_programs_name        ON programs (name);
CREATE INDEX idx_programs_category    ON programs (category_code);
CREATE INDEX idx_programs_delivery    ON programs (delivery_mode);
-- Tuỳ chọn nếu cần search q tốt hơn:
-- CREATE FULLTEXT INDEX ftx_programs_name_desc ON programs (name, description);

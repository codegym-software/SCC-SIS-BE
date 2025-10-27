-- V23: Tạo bảng class_journals (Nhật ký lớp học)
-- Cho phép giảng viên viết, sửa, xóa nhật ký về tiến độ giảng dạy

CREATE TABLE class_journals (
    journal_id      INT AUTO_INCREMENT PRIMARY KEY,
    
    class_id        INT NOT NULL COMMENT 'FK -> classes.class_id',
    teacher_id      INT NOT NULL COMMENT 'FK -> users.user_id (giảng viên viết nhật ký)',
    
    -- Nội dung nhật ký
    title           VARCHAR(500) NOT NULL COMMENT 'Tiêu đề nhật ký',
    content         TEXT NOT NULL COMMENT 'Nội dung chi tiết',
    journal_date    DATE NOT NULL COMMENT 'Ngày của nhật ký',
    
    -- Phân loại (optional - có thể mở rộng sau)
    journal_type    ENUM('PROGRESS','ANNOUNCEMENT','ISSUE','NOTE','OTHER') 
                    NOT NULL DEFAULT 'NOTE' 
                    COMMENT 'Loại nhật ký: tiến độ, thông báo, vấn đề, ghi chú, khác',
    
    -- Soft delete + audit
    deleted_at      DATETIME DEFAULT NULL COMMENT 'Thời điểm soft delete',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      INT DEFAULT NULL COMMENT 'ID user tạo nhật ký',
    updated_by      INT DEFAULT NULL COMMENT 'ID user cập nhật cuối',
    
    -- Ràng buộc dữ liệu
    CONSTRAINT chk_journal_title_length CHECK (CHAR_LENGTH(title) >= 3),
    CONSTRAINT chk_journal_content_length CHECK (CHAR_LENGTH(content) >= 10),
    
    -- Khóa ngoại
    CONSTRAINT fk_class_journals_class
        FOREIGN KEY (class_id) REFERENCES classes(class_id) ON DELETE CASCADE,
    CONSTRAINT fk_class_journals_teacher
        FOREIGN KEY (teacher_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_class_journals_created_by
        FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL,
    CONSTRAINT fk_class_journals_updated_by
        FOREIGN KEY (updated_by) REFERENCES users(user_id) ON DELETE SET NULL

) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'Nhật ký lớp học - Giảng viên ghi lại tiến độ/thông báo giảng dạy';

-- ===== Indexes cho performance =====

-- 1) List nhật ký theo lớp, sắp xếp theo ngày (mới nhất trước)
CREATE INDEX idx_journals_class_date_id 
    ON class_journals (class_id, journal_date DESC, journal_id DESC);

-- 2) List nhật ký theo giảng viên
CREATE INDEX idx_journals_teacher_date_id
    ON class_journals (teacher_id, journal_date DESC, journal_id DESC);

-- 3) Filter theo loại nhật ký
CREATE INDEX idx_journals_class_type_date
    ON class_journals (class_id, journal_type, journal_date DESC);

-- 4) Soft delete optimization (chỉ lấy chưa xóa)
CREATE INDEX idx_journals_deleted_at
    ON class_journals (deleted_at);

-- 5) Composite index cho filter class + không xóa + sort date
CREATE INDEX idx_journals_class_notdeleted_date
    ON class_journals (class_id, deleted_at, journal_date DESC);

-- ===== Trigger để validate giảng viên có quyền viết nhật ký =====

DELIMITER $$

CREATE TRIGGER trg_validate_journal_teacher_before_insert
BEFORE INSERT ON class_journals
FOR EACH ROW
BEGIN
    DECLARE teacher_assigned INT;
    
    -- Kiểm tra giảng viên có được phân công vào lớp này không
    -- (end_date IS NULL hoặc end_date >= journal_date)
    SELECT COUNT(*) INTO teacher_assigned
    FROM class_teachers
    WHERE class_id = NEW.class_id
      AND teacher_id = NEW.teacher_id
      AND start_date <= NEW.journal_date
      AND (end_date IS NULL OR end_date >= NEW.journal_date);
    
    IF teacher_assigned = 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Giảng viên không được phân công vào lớp này hoặc không còn hiệu lực tại ngày nhật ký';
    END IF;
END$$

CREATE TRIGGER trg_validate_journal_teacher_before_update
BEFORE UPDATE ON class_journals
FOR EACH ROW
BEGIN
    DECLARE teacher_assigned INT;
    
    -- Chỉ validate khi thay đổi class_id, teacher_id hoặc journal_date
    IF NEW.class_id <> OLD.class_id 
       OR NEW.teacher_id <> OLD.teacher_id 
       OR NEW.journal_date <> OLD.journal_date THEN
        
        SELECT COUNT(*) INTO teacher_assigned
        FROM class_teachers
        WHERE class_id = NEW.class_id
          AND teacher_id = NEW.teacher_id
          AND start_date <= NEW.journal_date
          AND (end_date IS NULL OR end_date >= NEW.journal_date);
        
        IF teacher_assigned = 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Giảng viên không được phân công vào lớp này hoặc không còn hiệu lực tại ngày nhật ký';
        END IF;
    END IF;
END$$

DELIMITER ;

-- V25: Sửa lỗi trigger V24 - Bảng user_roles không có cột deleted_at

-- Drop trigger lỗi từ V24
DROP TRIGGER IF EXISTS trg_validate_journal_teacher_before_insert;
DROP TRIGGER IF EXISTS trg_validate_journal_teacher_before_update;

-- Tạo lại trigger INSERT với logic ĐÚNG (bỏ ur.deleted_at)
DELIMITER $$

CREATE TRIGGER trg_validate_journal_teacher_before_insert
BEFORE INSERT ON class_journals
FOR EACH ROW
BEGIN
    DECLARE teacher_assigned INT DEFAULT 0;
    DECLARE is_super_admin INT DEFAULT 0;
    
    -- Bước 1: Kiểm tra user có phải SUPER_ADMIN không
    SELECT COUNT(*) INTO is_super_admin
    FROM user_roles ur
    JOIN roles r ON ur.role_id = r.role_id
    WHERE ur.user_id = NEW.teacher_id
      AND r.code = 'SUPER_ADMIN'
      AND r.deleted_at IS NULL;
    
    -- Bước 2: Nếu là SUPER_ADMIN → BỎ QUA validation
    IF is_super_admin > 0 THEN
        -- SUPER_ADMIN có thể tạo journal cho bất kỳ lớp nào
        SET teacher_assigned = 1;
    ELSE
        -- User thường (TEACHER) → phải kiểm tra phân công
        SELECT COUNT(*) INTO teacher_assigned
        FROM class_teachers
        WHERE class_id = NEW.class_id
          AND teacher_id = NEW.teacher_id
          AND start_date <= NEW.journal_date
          AND (end_date IS NULL OR end_date >= NEW.journal_date)
          AND deleted_at IS NULL;
    END IF;
    
    -- Bước 3: Nếu không hợp lệ → REJECT
    IF teacher_assigned = 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Giảng viên không được phân công vào lớp này hoặc không còn hiệu lực tại ngày nhật ký';
    END IF;
END$$

-- Tạo lại trigger UPDATE với logic ĐÚNG
CREATE TRIGGER trg_validate_journal_teacher_before_update
BEFORE UPDATE ON class_journals
FOR EACH ROW
BEGIN
    DECLARE teacher_assigned INT DEFAULT 0;
    DECLARE is_super_admin INT DEFAULT 0;
    
    -- Chỉ validate khi thay đổi class_id, teacher_id hoặc journal_date
    IF NEW.class_id <> OLD.class_id 
       OR NEW.teacher_id <> OLD.teacher_id 
       OR NEW.journal_date <> OLD.journal_date THEN
        
        -- Bước 1: Kiểm tra user có phải SUPER_ADMIN không
        SELECT COUNT(*) INTO is_super_admin
        FROM user_roles ur
        JOIN roles r ON ur.role_id = r.role_id
        WHERE ur.user_id = NEW.teacher_id
          AND r.code = 'SUPER_ADMIN'
          AND r.deleted_at IS NULL;
        
        -- Bước 2: Nếu là SUPER_ADMIN → BỎ QUA validation
        IF is_super_admin > 0 THEN
            SET teacher_assigned = 1;
        ELSE
            -- User thường → phải kiểm tra phân công
            SELECT COUNT(*) INTO teacher_assigned
            FROM class_teachers
            WHERE class_id = NEW.class_id
              AND teacher_id = NEW.teacher_id
              AND start_date <= NEW.journal_date
              AND (end_date IS NULL OR end_date >= NEW.journal_date)
              AND deleted_at IS NULL;
        END IF;
        
        -- Bước 3: Nếu không hợp lệ → REJECT
        IF teacher_assigned = 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Giảng viên không được phân công vào lớp này hoặc không còn hiệu lực tại ngày nhật ký';
        END IF;
    END IF;
END$$

DELIMITER ;

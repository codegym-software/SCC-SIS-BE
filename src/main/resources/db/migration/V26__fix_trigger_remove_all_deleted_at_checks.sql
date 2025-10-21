-- V26: Sửa lỗi trigger V25 - Bảng roles và user_roles KHÔNG có cột deleted_at
-- Chỉ giữ lại kiểm tra deleted_at cho class_teachers (bảng này CÓ deleted_at)

-- Drop trigger lỗi từ V25
DROP TRIGGER IF EXISTS trg_validate_journal_teacher_before_insert;
DROP TRIGGER IF EXISTS trg_validate_journal_teacher_before_update;

-- Tạo lại trigger INSERT - CHỈ CHECK deleted_at của class_teachers
DELIMITER $$

CREATE TRIGGER trg_validate_journal_teacher_before_insert
BEFORE INSERT ON class_journals
FOR EACH ROW
BEGIN
    DECLARE teacher_assigned INT DEFAULT 0;
    DECLARE is_super_admin INT DEFAULT 0;
    
    -- Bước 1: Kiểm tra user có phải SUPER_ADMIN không
    -- Không check deleted_at vì user_roles và roles không có cột này
    SELECT COUNT(*) INTO is_super_admin
    FROM user_roles ur
    JOIN roles r ON ur.role_id = r.role_id
    WHERE ur.user_id = NEW.teacher_id
      AND r.code = 'SUPER_ADMIN';
    
    -- Bước 2: Nếu là SUPER_ADMIN → BỎ QUA validation
    IF is_super_admin > 0 THEN
        -- SUPER_ADMIN có thể tạo journal cho bất kỳ lớp nào
        SET teacher_assigned = 1;
    ELSE
        -- User thường (TEACHER) → phải kiểm tra phân công
        -- Lưu ý: class_teachers KHÔNG có deleted_at, dùng end_date để check timeline
        SELECT COUNT(*) INTO teacher_assigned
        FROM class_teachers
        WHERE class_id = NEW.class_id
          AND teacher_id = NEW.teacher_id
          AND start_date <= NEW.journal_date
          AND (end_date IS NULL OR end_date >= NEW.journal_date);
    END IF;
    
    -- Bước 3: Nếu không hợp lệ → REJECT
    IF teacher_assigned = 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Giảng viên không được phân công vào lớp này hoặc không còn hiệu lực tại ngày nhật ký';
    END IF;
END$$

-- Tạo lại trigger UPDATE KHÔNG CHECK deleted_at
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
        -- Không check deleted_at vì user_roles và roles không có cột này
        SELECT COUNT(*) INTO is_super_admin
        FROM user_roles ur
        JOIN roles r ON ur.role_id = r.role_id
        WHERE ur.user_id = NEW.teacher_id
          AND r.code = 'SUPER_ADMIN';
        
        -- Bước 2: Nếu là SUPER_ADMIN → BỎ QUA validation
        IF is_super_admin > 0 THEN
            SET teacher_assigned = 1;
        ELSE
            -- User thường → phải kiểm tra phân công
            -- Lưu ý: class_teachers KHÔNG có deleted_at, dùng end_date để check timeline
            SELECT COUNT(*) INTO teacher_assigned
            FROM class_teachers
            WHERE class_id = NEW.class_id
              AND teacher_id = NEW.teacher_id
              AND start_date <= NEW.journal_date
              AND (end_date IS NULL OR end_date >= NEW.journal_date);
        END IF;
        
        -- Bước 3: Nếu không hợp lệ → REJECT
        IF teacher_assigned = 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Giảng viên không được phân công vào lớp này hoặc không còn hiệu lực tại ngày nhật ký';
        END IF;
    END IF;
END$$

DELIMITER ;

-- GIẢI THÍCH CẤU TRÚC DATABASE:
-- - Bảng user_roles: KHÔNG có deleted_at
-- - Bảng roles: KHÔNG có deleted_at  
-- - Bảng class_teachers: KHÔNG có deleted_at (dùng end_date để quản lý timeline)
-- - Trigger KHÔNG check deleted_at nữa, chỉ check timeline (start_date, end_date)

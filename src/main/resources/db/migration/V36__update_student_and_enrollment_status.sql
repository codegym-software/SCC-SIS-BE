-- =============================================================================
-- V36: Update Student and Enrollment Status Enums
-- =============================================================================
-- MỤC ĐÍCH:
--   Cập nhật enum values cho Student và Enrollment theo yêu cầu mới:
--   
--   STUDENT STATUS (overall_status):
--   - PENDING: đang chờ (mặc định khi tạo mới)
--   - ACTIVE: đang học (khi được gán vào lớp)
--   - DROPPED: nghỉ học
--   
--   ENROLLMENT STATUS (status):
--   - ACTIVE: đang học
--   - SUSPENDED: bảo lưu
--   - DROPPED: đã nghỉ
--   - GRADUATED: tốt nghiệp
-- =============================================================================

-- BƯỚC 1: CẬP NHẬT ENUM STUDENT STATUS
-- Thêm PENDING vào enum overall_status
ALTER TABLE students 
MODIFY COLUMN overall_status ENUM('PENDING', 'ACTIVE', 'INACTIVE', 'GRADUATED', 'SUSPENDED', 'DROPPED') 
DEFAULT 'PENDING' 
COMMENT 'Trạng thái tổng quát của học viên: PENDING=đang chờ, ACTIVE=đang học, DROPPED=nghỉ học';

-- Cập nhật các giá trị hiện tại
UPDATE students 
SET overall_status = 'PENDING' 
WHERE overall_status = 'ACTIVE' AND student_id NOT IN (
    SELECT DISTINCT student_id 
    FROM enrollments 
    WHERE status = 'ACTIVE' AND revoked_at IS NULL
);

-- BƯỚC 2: CẬP NHẬT ENUM ENROLLMENT STATUS  
-- Thêm GRADUATED vào enum status
ALTER TABLE enrollments 
MODIFY COLUMN status ENUM('ACTIVE', 'DROPPED', 'SUSPENDED', 'GRADUATED') 
DEFAULT 'ACTIVE' 
COMMENT 'Trạng thái ghi danh: ACTIVE=đang học, SUSPENDED=bảo lưu, DROPPED=đã nghỉ, GRADUATED=tốt nghiệp';

-- =============================================================================
-- BƯỚC 3: TẠO FUNCTION ĐỒNG BỘ TRẠNG THÁI
-- =============================================================================

DELIMITER //

-- Function để đồng bộ trạng thái Student khi Enrollment thay đổi
CREATE FUNCTION sync_student_status_from_enrollment(p_student_id INT) 
RETURNS VARCHAR(20)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE active_enrollments INT DEFAULT 0;
    DECLARE graduated_enrollments INT DEFAULT 0;
    DECLARE suspended_enrollments INT DEFAULT 0;
    DECLARE dropped_enrollments INT DEFAULT 0;
    
    -- Đếm các enrollment theo trạng thái
    SELECT 
        COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END),
        COUNT(CASE WHEN status = 'GRADUATED' THEN 1 END),
        COUNT(CASE WHEN status = 'SUSPENDED' THEN 1 END),
        COUNT(CASE WHEN status = 'DROPPED' THEN 1 END)
    INTO active_enrollments, graduated_enrollments, suspended_enrollments, dropped_enrollments
    FROM enrollments 
    WHERE student_id = p_student_id AND revoked_at IS NULL;
    
    -- Logic xác định trạng thái Student
    IF active_enrollments > 0 THEN
        RETURN 'ACTIVE';
    ELSEIF graduated_enrollments > 0 AND suspended_enrollments = 0 AND dropped_enrollments = 0 THEN
        RETURN 'GRADUATED';
    ELSEIF suspended_enrollments > 0 THEN
        RETURN 'PENDING';
    ELSEIF dropped_enrollments > 0 THEN
        RETURN 'DROPPED';
    ELSE
        RETURN 'PENDING';
    END IF;
END //

DELIMITER ;

-- =============================================================================
-- BƯỚC 4: TẠO TRIGGER ĐỒNG BỘ TRẠNG THÁI
-- =============================================================================

DELIMITER //

-- Trigger để tự động đồng bộ trạng thái Student khi Enrollment thay đổi
CREATE TRIGGER tr_enrollment_sync_student_status
    AFTER INSERT ON enrollments
    FOR EACH ROW
BEGIN
    UPDATE students 
    SET overall_status = sync_student_status_from_enrollment(NEW.student_id),
        updated_at = CURRENT_TIMESTAMP
    WHERE student_id = NEW.student_id;
END //

CREATE TRIGGER tr_enrollment_update_sync_student_status
    AFTER UPDATE ON enrollments
    FOR EACH ROW
BEGIN
    IF OLD.status != NEW.status OR OLD.revoked_at != NEW.revoked_at THEN
        UPDATE students 
        SET overall_status = sync_student_status_from_enrollment(NEW.student_id),
            updated_at = CURRENT_TIMESTAMP
        WHERE student_id = NEW.student_id;
    END IF;
END //

CREATE TRIGGER tr_enrollment_delete_sync_student_status
    AFTER DELETE ON enrollments
    FOR EACH ROW
BEGIN
    UPDATE students 
    SET overall_status = sync_student_status_from_enrollment(OLD.student_id),
        updated_at = CURRENT_TIMESTAMP
    WHERE student_id = OLD.student_id;
END //

DELIMITER ;

-- =============================================================================
-- BƯỚC 5: TẠO PROCEDURE TỰ ĐỘNG CHUYỂN TỐT NGHIỆP
-- =============================================================================

DELIMITER //

-- Procedure để tự động chuyển tất cả học viên trong lớp thành tốt nghiệp khi lớp hoàn thành
CREATE PROCEDURE sp_auto_graduate_class_students(IN p_class_id INT)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    
    START TRANSACTION;
    
    -- Chuyển tất cả enrollment ACTIVE trong lớp thành GRADUATED
    UPDATE enrollments 
    SET status = 'GRADUATED',
        left_at = CURRENT_DATE,
        updated_at = CURRENT_TIMESTAMP
    WHERE class_id = p_class_id 
      AND status = 'ACTIVE' 
      AND revoked_at IS NULL;
    
    -- Trigger sẽ tự động đồng bộ trạng thái Student
    
    COMMIT;
END //

DELIMITER ;

-- =============================================================================
-- BƯỚC 6: CẬP NHẬT DỮ LIỆU HIỆN TẠI
-- =============================================================================

-- Đồng bộ trạng thái Student dựa trên Enrollment hiện tại
UPDATE students s
SET overall_status = sync_student_status_from_enrollment(s.student_id),
    updated_at = CURRENT_TIMESTAMP
WHERE s.student_id IN (
    SELECT DISTINCT student_id 
    FROM enrollments 
    WHERE revoked_at IS NULL
);

-- =============================================================================
-- KẾT QUẢ SAU MIGRATION
-- =============================================================================
-- ✅ Student status: PENDING, ACTIVE, DROPPED (thêm PENDING)
-- ✅ Enrollment status: ACTIVE, SUSPENDED, DROPPED, GRADUATED (thêm GRADUATED)
-- ✅ Function sync_student_status_from_enrollment() để đồng bộ trạng thái
-- ✅ Triggers tự động đồng bộ khi Enrollment thay đổi
-- ✅ Procedure sp_auto_graduate_class_students() để tự động tốt nghiệp
-- ✅ Dữ liệu hiện tại đã được đồng bộ
-- =============================================================================

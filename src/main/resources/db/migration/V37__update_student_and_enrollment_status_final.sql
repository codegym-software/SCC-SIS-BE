-- =============================================================================
-- V37: Update Student and Enrollment Status Enums (Final)
-- =============================================================================
-- MỤC ĐÍCH:
--   Cập nhật enum values cho Student và Enrollment theo yêu cầu cuối cùng:
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
-- Đảm bảo chỉ có 3 trạng thái: PENDING, ACTIVE, DROPPED
ALTER TABLE students 
MODIFY COLUMN overall_status ENUM('PENDING', 'ACTIVE', 'DROPPED') 
DEFAULT 'PENDING' 
COMMENT 'Trạng thái tổng quát của học viên: PENDING=đang chờ, ACTIVE=đang học, DROPPED=nghỉ học';

-- Cập nhật các giá trị hiện tại không hợp lệ
UPDATE students 
SET overall_status = 'PENDING' 
WHERE overall_status NOT IN ('PENDING', 'ACTIVE', 'DROPPED');

-- BƯỚC 2: CẬP NHẬT ENUM ENROLLMENT STATUS  
-- Đảm bảo có 4 trạng thái: ACTIVE, SUSPENDED, DROPPED, GRADUATED
ALTER TABLE enrollments 
MODIFY COLUMN status ENUM('ACTIVE', 'SUSPENDED', 'DROPPED', 'GRADUATED') 
DEFAULT 'ACTIVE' 
COMMENT 'Trạng thái ghi danh: ACTIVE=đang học, SUSPENDED=bảo lưu, DROPPED=đã nghỉ, GRADUATED=tốt nghiệp';

-- Cập nhật các giá trị hiện tại không hợp lệ
UPDATE enrollments 
SET status = 'ACTIVE' 
WHERE status NOT IN ('ACTIVE', 'SUSPENDED', 'DROPPED', 'GRADUATED');

-- =============================================================================
-- BƯỚC 3: CẬP NHẬT FUNCTION ĐỒNG BỘ TRẠNG THÁI
-- =============================================================================

DELIMITER //

-- Drop function cũ nếu tồn tại
DROP FUNCTION IF EXISTS sync_student_status_from_enrollment //

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
        RETURN 'ACTIVE'; -- Tốt nghiệp vẫn coi là đang học cho đến khi có trạng thái mới
    ELSEIF suspended_enrollments > 0 THEN
        RETURN 'PENDING'; -- Bảo lưu coi như đang chờ
    ELSEIF dropped_enrollments > 0 THEN
        RETURN 'DROPPED';
    ELSE
        RETURN 'PENDING';
    END IF;
END //

DELIMITER ;

-- =============================================================================
-- BƯỚC 4: CẬP NHẬT TRIGGERS
-- =============================================================================

DELIMITER //

-- Drop triggers cũ nếu tồn tại
DROP TRIGGER IF EXISTS tr_enrollment_sync_student_status //
DROP TRIGGER IF EXISTS tr_enrollment_update_sync_student_status //
DROP TRIGGER IF EXISTS tr_enrollment_delete_sync_student_status //

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
-- BƯỚC 5: CẬP NHẬT PROCEDURE TỰ ĐỘNG CHUYỂN TỐT NGHIỆP
-- =============================================================================

DELIMITER //

-- Drop procedure cũ nếu tồn tại
DROP PROCEDURE IF EXISTS sp_auto_graduate_class_students //

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
-- KẾT QUẢ SAU MIGRATION V37
-- =============================================================================
-- ✅ Student status: PENDING (đang chờ), ACTIVE (đang học), DROPPED (nghỉ học)
-- ✅ Enrollment status: ACTIVE (đang học), SUSPENDED (bảo lưu), DROPPED (đã nghỉ), GRADUATED (tốt nghiệp)
-- ✅ Function sync_student_status_from_enrollment() đã cập nhật
-- ✅ Triggers tự động đồng bộ khi Enrollment thay đổi
-- ✅ Procedure sp_auto_graduate_class_students() để tự động tốt nghiệp
-- ✅ Dữ liệu hiện tại đã được đồng bộ
-- =============================================================================

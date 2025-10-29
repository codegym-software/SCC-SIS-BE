-- V40: Thêm trạng thái GRADUATED và sửa logic đồng bộ trạng thái học viên
-- Logic: ACTIVE > GRADUATED > DROPPED > SUSPENDED > PENDING
-- Đặc biệt: Nếu có GRADUATED (dù có DROPPED hay không) → GRADUATED

-- Thêm GRADUATED vào ENUM của bảng students
ALTER TABLE students 
MODIFY COLUMN overall_status ENUM('PENDING', 'ACTIVE', 'DROPPED', 'GRADUATED') 
DEFAULT 'PENDING' 
COMMENT 'Trạng thái tổng quát của học viên: PENDING=đang chờ, ACTIVE=đang học, DROPPED=nghỉ học, GRADUATED=tốt nghiệp';

DELIMITER //

-- Drop existing function if it exists
DROP FUNCTION IF EXISTS sync_student_status_from_enrollment //

-- Recreate function with updated logic
CREATE FUNCTION sync_student_status_from_enrollment(p_student_id INT)
RETURNS VARCHAR(20)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE v_overall_status VARCHAR(20) DEFAULT 'PENDING';
    DECLARE active_count INT DEFAULT 0;
    DECLARE dropped_count INT DEFAULT 0;
    DECLARE suspended_count INT DEFAULT 0;
    DECLARE graduated_count INT DEFAULT 0;

    -- Đếm số lượng enrollment theo từng trạng thái
    SELECT 
        COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END),
        COUNT(CASE WHEN status = 'DROPPED' THEN 1 END),
        COUNT(CASE WHEN status = 'SUSPENDED' THEN 1 END),
        COUNT(CASE WHEN status = 'GRADUATED' THEN 1 END)
    INTO active_count, dropped_count, suspended_count, graduated_count
    FROM enrollments 
    WHERE student_id = p_student_id AND revoked_at IS NULL;

    -- Logic theo thứ tự ưu tiên
    IF active_count > 0 THEN
        SET v_overall_status = 'ACTIVE'; -- Có ít nhất 1 lớp đang học
    ELSEIF graduated_count > 0 THEN
        -- Nếu có lớp tốt nghiệp (dù có DROPPED hay không) → GRADUATED
        -- Bao gồm cả trường hợp: TẤT CẢ đều GRADUATED hoặc có GRADUATED + DROPPED
        SET v_overall_status = 'GRADUATED';
    ELSEIF dropped_count > 0 THEN
        SET v_overall_status = 'DROPPED'; -- Chỉ có lớp đã nghỉ (không có GRADUATED)
    ELSEIF suspended_count > 0 THEN
        SET v_overall_status = 'PENDING'; -- Chỉ có lớp bảo lưu
    ELSE
        SET v_overall_status = 'PENDING'; -- Chưa có lớp nào
    END IF;

    RETURN v_overall_status;
END //

DELIMITER ;

-- Re-apply triggers to ensure they use the new function logic
-- Drop existing triggers if they exist
DROP TRIGGER IF EXISTS after_enrollments_insert;
DROP TRIGGER IF EXISTS after_enrollments_update;
DROP TRIGGER IF EXISTS after_enrollments_delete;

DELIMITER //

-- Trigger for INSERT on enrollments
CREATE TRIGGER after_enrollments_insert
AFTER INSERT ON enrollments
FOR EACH ROW
BEGIN
    UPDATE students
    SET overall_status = sync_student_status_from_enrollment(NEW.student_id)
    WHERE student_id = NEW.student_id;
END //

-- Trigger for UPDATE on enrollments
CREATE TRIGGER after_enrollments_update
AFTER UPDATE ON enrollments
FOR EACH ROW
BEGIN
    IF OLD.status <> NEW.status OR OLD.revoked_at <> NEW.revoked_at THEN
        UPDATE students
        SET overall_status = sync_student_status_from_enrollment(NEW.student_id)
        WHERE student_id = NEW.student_id;
    END IF;
END //

-- Trigger for DELETE on enrollments
CREATE TRIGGER after_enrollments_delete
AFTER DELETE ON enrollments
FOR EACH ROW
BEGIN
    UPDATE students
    SET overall_status = sync_student_status_from_enrollment(OLD.student_id)
    WHERE student_id = OLD.student_id;
END //

DELIMITER ;

-- Cập nhật lại tất cả trạng thái học viên theo logic mới
UPDATE students s
SET overall_status = sync_student_status_from_enrollment(s.student_id)
WHERE s.deleted_at IS NULL;
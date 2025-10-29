-- V39: Cập nhật logic đồng bộ trạng thái học viên theo yêu cầu mới
-- Logic: PENDING -> ACTIVE -> DROPPED/GRADUATED

DELIMITER //

-- Drop existing function if it exists
DROP FUNCTION IF EXISTS sync_student_status_from_enrollment //

-- Recreate function with updated logic theo yêu cầu
CREATE FUNCTION sync_student_status_from_enrollment(p_student_id INT)
RETURNS VARCHAR(20)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE v_overall_status VARCHAR(20) DEFAULT 'PENDING';

    -- Kiểm tra có enrollment nào không
    IF NOT EXISTS (SELECT 1 FROM enrollments WHERE student_id = p_student_id AND revoked_at IS NULL) THEN
        RETURN 'PENDING'; -- Chưa có lớp nào
    END IF;

    -- Kiểm tra có ACTIVE enrollments không
    IF EXISTS (SELECT 1 FROM enrollments WHERE student_id = p_student_id AND status = 'ACTIVE' AND revoked_at IS NULL) THEN
        SET v_overall_status = 'ACTIVE';
    -- Kiểm tra có GRADUATED enrollments không
    ELSEIF EXISTS (SELECT 1 FROM enrollments WHERE student_id = p_student_id AND status = 'GRADUATED' AND revoked_at IS NULL) THEN
        SET v_overall_status = 'ACTIVE'; -- Tốt nghiệp vẫn coi là đang học cho đến khi có trạng thái mới
    -- Kiểm tra có SUSPENDED enrollments không
    ELSEIF EXISTS (SELECT 1 FROM enrollments WHERE student_id = p_student_id AND status = 'SUSPENDED' AND revoked_at IS NULL) THEN
        SET v_overall_status = 'PENDING'; -- Bảo lưu coi như đang chờ
    -- Kiểm tra có DROPPED enrollments không
    ELSEIF EXISTS (SELECT 1 FROM enrollments WHERE student_id = p_student_id AND status = 'DROPPED' AND revoked_at IS NULL) THEN
        SET v_overall_status = 'DROPPED';
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

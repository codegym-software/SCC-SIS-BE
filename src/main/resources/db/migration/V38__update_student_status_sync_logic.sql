-- =============================================================================
-- V38: Update Student Status Sync Logic for Class-Specific Display
-- =============================================================================
-- MỤC ĐÍCH:
--   Cập nhật logic đồng bộ trạng thái Student để phù hợp với hiển thị theo từng lớp
--   Giữ nguyên 3 trạng thái: PENDING, ACTIVE, DROPPED
--   Logic mới: Ưu tiên ACTIVE > SUSPENDED > DROPPED
-- =============================================================================

-- BƯỚC 1: CẬP NHẬT FUNCTION ĐỒNG BỘ TRẠNG THÁI
DELIMITER //

-- Drop function cũ nếu tồn tại
DROP FUNCTION IF EXISTS sync_student_status_from_enrollment //

-- Function mới: Tính overall_status từ enrollments với logic ưu tiên rõ ràng
CREATE FUNCTION sync_student_status_from_enrollment(p_student_id INT) 
RETURNS VARCHAR(20)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE active_enrollments INT DEFAULT 0;
    DECLARE suspended_enrollments INT DEFAULT 0;
    DECLARE dropped_enrollments INT DEFAULT 0;
    DECLARE graduated_enrollments INT DEFAULT 0;
    
    -- Đếm các enrollment theo trạng thái
    SELECT 
        COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END),
        COUNT(CASE WHEN status = 'SUSPENDED' THEN 1 END),
        COUNT(CASE WHEN status = 'DROPPED' THEN 1 END),
        COUNT(CASE WHEN status = 'GRADUATED' THEN 1 END)
    INTO active_enrollments, suspended_enrollments, dropped_enrollments, graduated_enrollments
    FROM enrollments 
    WHERE student_id = p_student_id AND revoked_at IS NULL;
    
    -- Logic ưu tiên: ACTIVE > SUSPENDED > DROPPED
    -- Nếu có ít nhất 1 lớp đang học -> ACTIVE
    IF active_enrollments > 0 THEN
        RETURN 'ACTIVE';
    -- Nếu có lớp bảo lưu và không có lớp đang học -> PENDING
    ELSEIF suspended_enrollments > 0 THEN
        RETURN 'PENDING';
    -- Nếu có lớp đã nghỉ và không có lớp đang học/bảo lưu -> DROPPED
    ELSEIF dropped_enrollments > 0 THEN
        RETURN 'DROPPED';
    -- Nếu chỉ có lớp tốt nghiệp -> ACTIVE (coi như hoàn thành)
    ELSEIF graduated_enrollments > 0 THEN
        RETURN 'ACTIVE';
    -- Không có enrollment nào -> PENDING
    ELSE
        RETURN 'PENDING';
    END IF;
END //

DELIMITER ;

-- =============================================================================
-- BƯỚC 2: CẬP NHẬT TRIGGERS (GIỮ NGUYÊN)
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
-- BƯỚC 3: TẠO VIEW ĐỂ HỖ TRỢ FRONTEND
-- =============================================================================

-- View để lấy thông tin student với enrollments chi tiết
CREATE VIEW student_enrollment_details AS
SELECT 
    s.student_id,
    s.full_name,
    s.email,
    s.phone,
    s.dob,
    s.gender,
    s.national_id_no,
    s.address_line,
    s.province,
    s.district,
    s.ward,
    s.note,
    s.overall_status,
    s.created_at,
    s.updated_at,
    e.enrollment_id,
    e.class_id,
    c.name as class_name,
    p.name as program_name,
    e.status as enrollment_status,
    e.enrolled_at,
    e.left_at,
    e.note as enrollment_note
FROM students s
LEFT JOIN enrollments e ON s.student_id = e.student_id AND e.revoked_at IS NULL
LEFT JOIN classes c ON e.class_id = c.class_id
LEFT JOIN programs p ON c.program_id = p.program_id;

-- =============================================================================
-- BƯỚC 4: CẬP NHẬT DỮ LIỆU HIỆN TẠI
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
-- KẾT QUẢ SAU MIGRATION V38
-- =============================================================================
-- ✅ Function sync_student_status_from_enrollment() với logic ưu tiên mới
-- ✅ Triggers tự động đồng bộ khi Enrollment thay đổi
-- ✅ View student_enrollment_details để hỗ trợ frontend
-- ✅ Logic: ACTIVE > SUSPENDED > DROPPED
-- ✅ Dữ liệu hiện tại đã được đồng bộ
-- =============================================================================

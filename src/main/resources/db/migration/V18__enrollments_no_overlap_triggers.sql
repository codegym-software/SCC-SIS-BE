-- V18__enrollments_no_overlap_triggers.sql
-- Triggers chống chồng lấn thời gian (cùng class_id, student_id)

DELIMITER $$

CREATE TRIGGER trg_enroll_no_overlap_ins
    BEFORE INSERT ON enrollments
    FOR EACH ROW
BEGIN
    DECLARE cnt INT;
    SELECT COUNT(*) INTO cnt
    FROM enrollments e
    WHERE e.class_id   = NEW.class_id
      AND e.student_id = NEW.student_id
      AND NEW.enrolled_at <= COALESCE(e.left_at, DATE '9999-12-31')
      AND e.enrolled_at   <= COALESCE(NEW.left_at, DATE '9999-12-31');
    IF cnt > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Ghi danh trùng lấn thời gian cho (class_id, student_id)';
END IF;
END$$

CREATE TRIGGER trg_enroll_no_overlap_upd
    BEFORE UPDATE ON enrollments
    FOR EACH ROW
BEGIN
    DECLARE cnt INT;
    SELECT COUNT(*) INTO cnt
    FROM enrollments e
    WHERE e.class_id   = NEW.class_id
      AND e.student_id = NEW.student_id
      AND e.enrollment_id <> OLD.enrollment_id
      AND NEW.enrolled_at <= COALESCE(e.left_at, DATE '9999-12-31')
      AND e.enrolled_at   <= COALESCE(NEW.left_at, DATE '9999-12-31');
    IF cnt > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Ghi danh trùng lấn thời gian cho (class_id, student_id)';
END IF;
END$$

DELIMITER ;

-- V15__enforce_no_overlap_class_teachers.sql
-- Triggers chống chồng lấn thời gian cho cùng (class_id, teacher_id)

CREATE TRIGGER trg_ct_no_overlap_ins
    BEFORE INSERT ON class_teachers
    FOR EACH ROW
BEGIN
    DECLARE cnt INT;
    SELECT COUNT(*) INTO cnt
    FROM class_teachers t
    WHERE t.class_id = NEW.class_id
      AND t.teacher_id = NEW.teacher_id
      AND NEW.start_date <= COALESCE(t.end_date, '9999-12-31')
      AND t.start_date <= COALESCE(NEW.end_date, '9999-12-31');
    IF cnt > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Overlapping assignment for (class_id, teacher_id)';
END IF;
END;

CREATE TRIGGER trg_ct_no_overlap_upd
    BEFORE UPDATE ON class_teachers
    FOR EACH ROW
BEGIN
    DECLARE cnt INT;
    SELECT COUNT(*) INTO cnt
    FROM class_teachers t
    WHERE t.class_id = NEW.class_id
      AND t.teacher_id = NEW.teacher_id
      AND t.class_teacher_id <> OLD.class_teacher_id
      AND NEW.start_date <= COALESCE(t.end_date, '9999-12-31')
      AND t.start_date <= COALESCE(NEW.end_date, '9999-12-31');
    IF cnt > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Overlapping assignment for (class_id, teacher_id)';
END IF;
END;
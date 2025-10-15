-- V21__add_study_day_and_time_to_classes.sql
-- Thêm ngày học (JSON array) và giờ học (enum 3 ca)

ALTER TABLE classes
    ADD COLUMN study_days JSON NULL COMMENT 'Các ngày học trong tuần dạng JSON array, ví dụ: ["MONDAY", "THURSDAY"]',
    ADD COLUMN study_time ENUM('MORNING', 'AFTERNOON', 'EVENING')
        NULL COMMENT 'Ca học: MORNING (8:00-11:00), AFTERNOON (14:00-17:00), EVENING (18:00-21:00)';

-- Index để tối ưu tìm kiếm theo giờ học
CREATE INDEX idx_classes_study_time ON classes (study_time);

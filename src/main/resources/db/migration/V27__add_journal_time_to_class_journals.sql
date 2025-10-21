-- V27__add_journal_time_to_class_journals.sql
-- Thêm trường journal_time (TIME) để lưu giờ:phút ghi nhật ký

ALTER TABLE class_journals
    ADD COLUMN journal_time TIME NULL COMMENT 'Giờ:phút ghi nhật ký (HH:mm)';

-- Cập nhật dữ liệu cũ: set journal_time = 00:00 cho các nhật ký đã có
UPDATE class_journals 
SET journal_time = '00:00:00' 
WHERE journal_time IS NULL AND deleted_at IS NULL;

-- Comment lại ý nghĩa của journal_date
ALTER TABLE class_journals 
MODIFY COLUMN journal_date DATE NOT NULL COMMENT 'Ngày ghi nhật ký (YYYY-MM-DD)';

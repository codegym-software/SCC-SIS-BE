-- =============================================================================
-- V31: Thay đổi syllabus_url từ VARCHAR(500) sang TEXT để lưu JSON array
-- =============================================================================
-- Mục đích: Cho phép lưu nhiều tài liệu học tập (files/links) thay vì 1 URL duy nhất
-- Format JSON: [{"url":"...","fileName":"...","fileType":"...","fileSize":123,"uploadedAt":"...","uploadedBy":1}]
-- =============================================================================

ALTER TABLE modules
    MODIFY COLUMN syllabus_url TEXT DEFAULT NULL COMMENT 'JSON array chứa nhiều tài liệu học tập';

-- Cập nhật comment cho has_syllabus
ALTER TABLE modules
    MODIFY COLUMN has_syllabus BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'TRUE nếu có ít nhất 1 tài liệu';

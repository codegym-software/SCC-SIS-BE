-- ============================================
-- MIGRATION: V47__add_google_drive_content_type.sql
-- Thêm GOOGLE_DRIVE vào content_type enum
-- ============================================

-- Thêm giá trị GOOGLE_DRIVE vào ENUM content_type
ALTER TABLE lessons 
MODIFY COLUMN content_type ENUM('VIMEO', 'LOCAL_FILE', 'EXTERNAL_URL', 'GOOGLE_DRIVE', 'YOUTUBE') DEFAULT 'VIMEO';

-- Note: GOOGLE_DRIVE - Link Google Drive embed
-- Note: YOUTUBE - Link YouTube embed
-- Note: EXTERNAL_URL - Các link external khác (OneDrive, Dropbox, etc.)

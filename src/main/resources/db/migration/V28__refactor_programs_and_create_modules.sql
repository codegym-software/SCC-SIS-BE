-- =============================================================================
-- V28: Refactor Programs & Create Modules Table
-- =============================================================================
-- MỤC ĐÍCH:
--   1. Di chuyển trường `level` từ bảng programs sang modules
--      (Vì level là thuộc tính của MÔN HỌC, không phải KHÓA HỌC)
--   
--   2. Tạo bảng modules để quản lý chi tiết các môn học trong mỗi khóa
--
-- QUAN HỆ:
--   Program (1) ──< (N) Modules
--   Một khóa học có nhiều môn học
--   Mỗi môn học CHỈ THUỘC MỘT khóa học
-- =============================================================================


-- =============================================================================
-- BƯỚC 1: XÓA CỘT `level` KHỎI BẢNG PROGRAMS (NẾU TỒN TẠI)
-- =============================================================================

-- Xóa cột level nếu tồn tại
SET @dbname = DATABASE();
SET @tablename = 'programs';
SET @columnname = 'level';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE 
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  'ALTER TABLE programs DROP COLUMN level;',
  'SELECT 1;'
));
PREPARE alterIfExists FROM @preparedStatement;
EXECUTE alterIfExists;
DEALLOCATE PREPARE alterIfExists;


-- =============================================================================
-- BƯỚC 2: TẠO BẢNG MODULES (MÔN HỌC TRONG KHÓA)
-- =============================================================================

CREATE TABLE modules (
    module_id       INT AUTO_INCREMENT PRIMARY KEY,
    
    -- ===== LIÊN KẾT VỚI PROGRAM =====
    
    program_id      INT NOT NULL COMMENT 'ID khóa học chứa module này',
    
    -- ===== ĐỊNH DANH MÔN HỌC =====
    
    code            VARCHAR(50) NOT NULL COMMENT 'Mã môn học (VD: MOD001, JAVA_CORE)',
    name            VARCHAR(255) NOT NULL COMMENT 'Tên môn học (VD: Nhập môn tư duy lập trình)',
    description     TEXT COMMENT 'Mô tả chi tiết nội dung môn học',
    
    -- ===== SẮP XẾP TRONG KHÓA HỌC =====
    
    sequence_order  INT UNSIGNED NOT NULL COMMENT 'Thứ tự học trong khóa (1, 2, 3...)',
    semester        INT UNSIGNED DEFAULT NULL COMMENT 'Học kỳ/tháng đề xuất (1, 2, 3...)',
    
    -- ===== THÔNG TIN HỌC TẬP =====
    
    credits         INT UNSIGNED NOT NULL COMMENT 'Số tín chỉ của môn học (1-10)',
    duration_hours  INT UNSIGNED DEFAULT NULL COMMENT 'Số giờ học của môn này',
    level           VARCHAR(50) DEFAULT NULL COMMENT 'Độ KHÓ môn học (Beginner/Intermediate/Advanced)',
    is_mandatory    BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'TRUE=Bắt buộc, FALSE=Tự chọn',
    
    -- ===== ĐỀ CƯƠNG (SYLLABUS) =====
    
    syllabus_url    VARCHAR(500) DEFAULT NULL COMMENT 'URL file đề cương (PDF/DOCX/Drive)',
    has_syllabus    BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Đã có đề cương đầy đủ',
    
    -- ===== GHI CHÚ =====
    
    notes           TEXT DEFAULT NULL COMMENT 'Ghi chú đặc biệt về module',
    
    -- ===== TRẠNG THÁI & SOFT DELETE =====
    
    is_active       BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Module đang hoạt động',
    deleted_at      DATETIME DEFAULT NULL COMMENT 'Soft delete timestamp',
    
    -- ===== AUDIT =====
    
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      INT DEFAULT NULL,
    updated_by      INT DEFAULT NULL,
    
    -- ===== CONSTRAINTS =====
    
    CONSTRAINT chk_module_credits_range 
        CHECK (credits > 0 AND credits <= 10),
    
    CONSTRAINT chk_module_duration_positive 
        CHECK (duration_hours IS NULL OR duration_hours > 0),
    
    CONSTRAINT chk_module_sequence_positive 
        CHECK (sequence_order > 0),
    
    CONSTRAINT chk_module_semester_positive 
        CHECK (semester IS NULL OR semester > 0),
    
    CONSTRAINT chk_module_deleted_implies_inactive 
        CHECK (deleted_at IS NULL OR is_active = FALSE),
    
    -- ===== FOREIGN KEYS =====
    
    CONSTRAINT fk_modules_program 
        FOREIGN KEY (program_id) REFERENCES programs(program_id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_modules_created_by 
        FOREIGN KEY (created_by) REFERENCES users(user_id) 
        ON DELETE SET NULL,
    
    CONSTRAINT fk_modules_updated_by 
        FOREIGN KEY (updated_by) REFERENCES users(user_id) 
        ON DELETE SET NULL,
    
    -- ===== UNIQUE CONSTRAINTS =====
    
    CONSTRAINT uk_module_code_per_program 
        UNIQUE (program_id, code),
    
    CONSTRAINT uk_module_sequence_per_program 
        UNIQUE (program_id, sequence_order)
);


-- =============================================================================
-- BƯỚC 3: TẠO INDEXES
-- =============================================================================

CREATE INDEX idx_modules_program       ON modules (program_id);
CREATE INDEX idx_modules_is_active     ON modules (is_active);
CREATE INDEX idx_modules_deleted_at    ON modules (deleted_at);
CREATE INDEX idx_modules_name          ON modules (name);
CREATE INDEX idx_modules_level         ON modules (level);
CREATE INDEX idx_modules_sequence      ON modules (program_id, sequence_order);








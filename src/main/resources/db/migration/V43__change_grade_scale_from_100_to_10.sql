-- =============================================================================
-- V43: THAY ĐỔI THANG ĐIỂM TỪ 0-100 SANG 0-10
-- =============================================================================
-- Mục đích: Thay đổi thang điểm từ 0-100 sang 0-10 để phù hợp với yêu cầu
-- Final Score = Theory (30%) + Practice (70%)
-- Pass Status: PASS nếu final_score >= 5, FAIL nếu < 5

-- =============================================================================
-- BƯỚC 1: XÓA GENERATED COLUMNS (final_score và pass_status)
-- =============================================================================
-- Xóa pass_status trước vì nó phụ thuộc vào final_score
SET @db := DATABASE();
SET @col_exists := (
  SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA=@db AND TABLE_NAME='grade_records' AND COLUMN_NAME='pass_status'
  LIMIT 1
);
SET @sql := IF(@col_exists IS NOT NULL,
  'ALTER TABLE grade_records DROP COLUMN pass_status',
  'DO 0'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @col_exists := (
  SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA=@db AND TABLE_NAME='grade_records' AND COLUMN_NAME='final_score'
  LIMIT 1
);
SET @sql := IF(@col_exists IS NOT NULL,
  'ALTER TABLE grade_records DROP COLUMN final_score',
  'DO 0'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- =============================================================================
-- BƯỚC 2: XÓA CHECK CONSTRAINTS CŨ (0-100)
-- =============================================================================
-- MySQL 8.0.16+ hỗ trợ DROP CHECK, nhưng không hỗ trợ IF EXISTS
-- Thử xóa và bỏ qua lỗi nếu không tồn tại
SET @check_exists := (
  SELECT 1 FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
  WHERE TABLE_SCHEMA=@db 
    AND TABLE_NAME='grade_records' 
    AND CONSTRAINT_NAME='chk_grade_records_theory_range'
  LIMIT 1
);
SET @sql := IF(@check_exists IS NOT NULL,
  'ALTER TABLE grade_records DROP CHECK chk_grade_records_theory_range',
  'DO 0'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @check_exists := (
  SELECT 1 FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
  WHERE TABLE_SCHEMA=@db 
    AND TABLE_NAME='grade_records' 
    AND CONSTRAINT_NAME='chk_grade_records_practice_range'
  LIMIT 1
);
SET @sql := IF(@check_exists IS NOT NULL,
  'ALTER TABLE grade_records DROP CHECK chk_grade_records_practice_range',
  'DO 0'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- =============================================================================
-- BƯỚC 3: CHUYỂN ĐỔI DỮ LIỆU CŨ TỪ 0-100 SANG 0-10 (nếu có)
-- =============================================================================
-- Chia tất cả điểm hiện có cho 10 để chuyển từ thang 0-100 sang 0-10
UPDATE grade_records
SET theory_score = theory_score / 10,
    practice_score = practice_score / 10
WHERE theory_score IS NOT NULL OR practice_score IS NOT NULL;

-- =============================================================================
-- BƯỚC 4: THAY ĐỔI COMMENT VÀ THÊM CHECK CONSTRAINTS MỚI (0-10)
-- =============================================================================
ALTER TABLE grade_records
   MODIFY COLUMN theory_score DECIMAL(5,2) DEFAULT NULL 
   COMMENT 'Điểm lý thuyết (0-10), NULL nếu chưa nhập';

ALTER TABLE grade_records
   MODIFY COLUMN practice_score DECIMAL(5,2) DEFAULT NULL 
   COMMENT 'Điểm thực hành (0-10), NULL nếu chưa nhập';

-- Thêm CHECK constraints mới cho thang 0-10
ALTER TABLE grade_records
   ADD CONSTRAINT chk_grade_records_theory_range
       CHECK (theory_score IS NULL OR (theory_score >= 0 AND theory_score <= 10));

ALTER TABLE grade_records
   ADD CONSTRAINT chk_grade_records_practice_range
       CHECK (practice_score IS NULL OR (practice_score >= 0 AND practice_score <= 10));

-- =============================================================================
-- BƯỚC 5: TẠO LẠI GENERATED COLUMNS VỚI LOGIC MỚI (0-10)
-- =============================================================================
-- Final Score = Theory (30%) + Practice (70%) - thang 0-10
ALTER TABLE grade_records
   ADD COLUMN final_score DECIMAL(5,2) GENERATED ALWAYS AS (
       CASE
           WHEN theory_score IS NOT NULL AND practice_score IS NOT NULL
           THEN (theory_score * 0.3 + practice_score * 0.7)
           ELSE NULL
       END
   ) VIRTUAL
   COMMENT 'Điểm tổng hợp: Lý thuyết 30% + Thực hành 70% (thang 0-10)';

-- Pass Status: PASS nếu final_score >= 5, FAIL nếu < 5
ALTER TABLE grade_records
   ADD COLUMN pass_status ENUM('PASS', 'FAIL') GENERATED ALWAYS AS (
       CASE
           WHEN theory_score IS NOT NULL AND practice_score IS NOT NULL THEN
               CASE
                   WHEN (theory_score * 0.3 + practice_score * 0.7) >= 5 THEN 'PASS'
                   ELSE 'FAIL'
               END
           ELSE NULL
       END
   ) VIRTUAL
   COMMENT 'Trạng thái: PASS nếu >= 5, FAIL nếu < 5 (thang 0-10)';


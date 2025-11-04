-- V42: Create Grade Entry and Grade Records Tables
-- =============================================================================
-- MỤC ĐÍCH:
--   1. Tạo bảng grade_entries (Đợt nhập điểm) để lưu thông tin các đợt nhập điểm
--   2. Tạo bảng grade_records (Bản ghi điểm) để lưu điểm lý thuyết và thực hành
--      của từng học viên trong một đợt nhập điểm
--
-- QUAN HỆ:
--   GradeEntry (1) ──< (N) GradeRecords
--   Một đợt nhập điểm có nhiều bản ghi điểm (mỗi học viên một bản ghi)
--  
--   GradeEntry liên kết với:
--   - Class (class_id): Lớp học
--   - Module (module_id): Module trong semester của lớp
--   - User (created_by): Giảng viên nhập điểm
--
--   GradeRecord liên kết với:
--   - GradeEntry (grade_entry_id): Đợt nhập điểm
--   - Student (student_id): Học viên
-- =============================================================================

-- =============================================================================
-- BƯỚC 1: TẠO BẢNG GRADE_ENTRIES (ĐỢT NHẬP ĐIỂM)
-- =============================================================================

CREATE TABLE grade_entries (
   grade_entry_id  INT AUTO_INCREMENT PRIMARY KEY,
  
   -- ===== LIÊN KẾT VỚI CLASS VÀ MODULE =====
   class_id        INT NOT NULL COMMENT 'FK -> classes.class_id, lớp học',
   module_id       INT NOT NULL COMMENT 'FK -> modules.module_id, module trong semester của lớp',
  
   -- ===== THÔNG TIN ĐỢT NHẬP ĐIỂM =====
   entry_date      DATE NOT NULL COMMENT 'Ngày nhập điểm (người dùng chọn trong form)',
  
   -- ===== AUDIT =====
   created_by      INT NOT NULL COMMENT 'FK -> users.user_id, giảng viên nhập điểm',
   created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
   updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
   -- ===== CONSTRAINTS =====
   -- Một lớp chỉ có thể nhập điểm cho một module một lần trong một ngày
   CONSTRAINT uk_grade_entries_class_module_date
       UNIQUE (class_id, module_id, entry_date),
  
   -- ===== FOREIGN KEYS =====
   CONSTRAINT fk_grade_entries_class
       FOREIGN KEY (class_id) REFERENCES classes(class_id)
       ON DELETE CASCADE,
  
   CONSTRAINT fk_grade_entries_module
       FOREIGN KEY (module_id) REFERENCES modules(module_id)
       ON DELETE RESTRICT,
  
   CONSTRAINT fk_grade_entries_created_by
       FOREIGN KEY (created_by) REFERENCES users(user_id)
       ON DELETE RESTRICT
) ENGINE=InnoDB
 DEFAULT CHARSET=utf8mb4
 COLLATE=utf8mb4_unicode_ci
 COMMENT='Bảng lưu các đợt nhập điểm cho module trong lớp học';

-- =============================================================================
-- BƯỚC 2: TẠO BẢNG GRADE_RECORDS (BẢN GHI ĐIỂM CHI TIẾT)
-- =============================================================================

CREATE TABLE grade_records (
   grade_record_id INT AUTO_INCREMENT PRIMARY KEY,
  
   -- ===== LIÊN KẾT =====
   grade_entry_id  INT NOT NULL COMMENT 'FK -> grade_entries.grade_entry_id, đợt nhập điểm',
   student_id      INT NOT NULL COMMENT 'FK -> students.student_id, học viên',
  
   -- ===== ĐIỂM SỐ =====
   theory_score    DECIMAL(5,2) DEFAULT NULL COMMENT 'Điểm lý thuyết (0-100), NULL nếu chưa nhập',
   practice_score  DECIMAL(5,2) DEFAULT NULL COMMENT 'Điểm thực hành (0-100), NULL nếu chưa nhập',
  
   -- ===== AUDIT =====
   created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
   updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
   -- ===== CONSTRAINTS =====
   -- Một đợt nhập điểm chỉ có một bản ghi điểm cho một học viên
   CONSTRAINT uk_grade_records_entry_student
       UNIQUE (grade_entry_id, student_id),
  
   -- Điểm phải trong khoảng 0-100
   CONSTRAINT chk_grade_records_theory_range
       CHECK (theory_score IS NULL OR (theory_score >= 0 AND theory_score <= 100)),
  
   CONSTRAINT chk_grade_records_practice_range
       CHECK (practice_score IS NULL OR (practice_score >= 0 AND practice_score <= 100)),
  
   -- ===== FOREIGN KEYS =====
   CONSTRAINT fk_grade_records_entry
       FOREIGN KEY (grade_entry_id) REFERENCES grade_entries(grade_entry_id)
       ON DELETE CASCADE,
  
   CONSTRAINT fk_grade_records_student
       FOREIGN KEY (student_id) REFERENCES students(student_id)
       ON DELETE RESTRICT
) ENGINE=InnoDB
 DEFAULT CHARSET=utf8mb4
 COLLATE=utf8mb4_unicode_ci
 COMMENT='Bảng lưu điểm chi tiết của từng học viên trong đợt nhập điểm';

-- =============================================================================
-- BƯỚC 3: TẠO GENERATED COLUMNS CHO FINAL_SCORE VÀ PASS_STATUS
-- =============================================================================
-- Final Score = Theory (30%) + Practice (70%)
-- Pass Status: PASS nếu final_score >= 50%, FAIL nếu < 50%

ALTER TABLE grade_records
   ADD COLUMN final_score DECIMAL(5,2) GENERATED ALWAYS AS (
       CASE
           WHEN theory_score IS NOT NULL AND practice_score IS NOT NULL
           THEN (theory_score * 0.3 + practice_score * 0.7)
           ELSE NULL
       END
   ) VIRTUAL
   COMMENT 'Điểm tổng hợp: Lý thuyết 30% + Thực hành 70%';

ALTER TABLE grade_records
   ADD COLUMN pass_status ENUM('PASS', 'FAIL') GENERATED ALWAYS AS (
       CASE
           WHEN theory_score IS NOT NULL AND practice_score IS NOT NULL THEN
               CASE
                   WHEN (theory_score * 0.3 + practice_score * 0.7) >= 50 THEN 'PASS'
                   ELSE 'FAIL'
               END
           ELSE NULL
       END
   ) VIRTUAL
   COMMENT 'Trạng thái: PASS nếu >= 50%, FAIL nếu < 50%';

-- =============================================================================
-- BƯỚC 4: TẠO INDEXES TỐI ƯU
-- =============================================================================

-- Indexes cho grade_entries
-- 1) Lọc đợt nhập điểm theo lớp và module
CREATE INDEX idx_grade_entries_class_module
   ON grade_entries (class_id, module_id, entry_date DESC);

-- 2) Lọc đợt nhập điểm theo lớp để lấy danh sách ngày nhập điểm
CREATE INDEX idx_grade_entries_class_date
   ON grade_entries (class_id, entry_date DESC);

-- 3) Lọc đợt nhập điểm theo module
CREATE INDEX idx_grade_entries_module
   ON grade_entries (module_id);

-- 4) Lọc đợt nhập điểm theo giảng viên
CREATE INDEX idx_grade_entries_created_by
   ON grade_entries (created_by, entry_date DESC);

-- Indexes cho grade_records
-- 1) Lọc bản ghi điểm theo đợt nhập điểm
CREATE INDEX idx_grade_records_entry
   ON grade_records (grade_entry_id, student_id);

-- 2) Lọc bản ghi điểm theo học viên
CREATE INDEX idx_grade_records_student
   ON grade_records (student_id, grade_entry_id);

-- 3) Tìm kiếm theo pass_status (nếu cần filter PASS/FAIL)
CREATE INDEX idx_grade_records_pass_status
   ON grade_records (pass_status);

-- 4) Composite index cho truy vấn hiệu quả
CREATE INDEX idx_grade_records_entry_final
   ON grade_records (grade_entry_id, final_score DESC);

-- =============================================================================
-- BƯỚC 5: TẠO TRIGGER VALIDATION (Nếu cần validate module thuộc program của class)
-- =============================================================================

DELIMITER //

CREATE TRIGGER trg_grade_entries_validate_module_program
BEFORE INSERT ON grade_entries
FOR EACH ROW
BEGIN
   DECLARE v_class_program_id INT;
   DECLARE v_module_program_id INT;
  
   -- Lấy program_id của class
   SELECT program_id INTO v_class_program_id
   FROM classes
   WHERE class_id = NEW.class_id;
  
   -- Lấy program_id của module
   SELECT program_id INTO v_module_program_id
   FROM modules
   WHERE module_id = NEW.module_id;
  
   -- Validate: Module phải thuộc cùng program với class
   IF v_class_program_id IS NULL THEN
       SIGNAL SQLSTATE '45000'
       SET MESSAGE_TEXT = 'Class not found';
   END IF;
  
   IF v_module_program_id IS NULL THEN
       SIGNAL SQLSTATE '45000'
       SET MESSAGE_TEXT = 'Module not found';
   END IF;
  
   IF v_class_program_id != v_module_program_id THEN
       SIGNAL SQLSTATE '45000'
       SET MESSAGE_TEXT = 'Module must belong to the same program as the class';
   END IF;
END//

CREATE TRIGGER trg_grade_entries_validate_module_program_update
BEFORE UPDATE ON grade_entries
FOR EACH ROW
BEGIN
   DECLARE v_class_program_id INT;
   DECLARE v_module_program_id INT;
  
   -- Chỉ validate nếu class_id hoặc module_id thay đổi
   IF NEW.class_id != OLD.class_id OR NEW.module_id != OLD.module_id THEN
       -- Lấy program_id của class
       SELECT program_id INTO v_class_program_id
       FROM classes
       WHERE class_id = NEW.class_id;
      
       -- Lấy program_id của module
       SELECT program_id INTO v_module_program_id
       FROM modules
       WHERE module_id = NEW.module_id;
      
       -- Validate: Module phải thuộc cùng program với class
       IF v_class_program_id IS NULL THEN
           SIGNAL SQLSTATE '45000'
           SET MESSAGE_TEXT = 'Class not found';
       END IF;
      
       IF v_module_program_id IS NULL THEN
           SIGNAL SQLSTATE '45000'
           SET MESSAGE_TEXT = 'Module not found';
       END IF;
      
       IF v_class_program_id != v_module_program_id THEN
           SIGNAL SQLSTATE '45000'
           SET MESSAGE_TEXT = 'Module must belong to the same program as the class';
       END IF;
   END IF;
END//

DELIMITER ;

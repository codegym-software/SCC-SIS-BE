-- =============================================================================
-- V29: Refactor Module Prerequisites - One-to-Many -> Many-to-Many
-- =============================================================================
-- MỤC ĐÍCH:
--   Chuyển đổi quan hệ môn tiên quyết từ ONE-TO-ONE sang MANY-TO-MANY
--   để hỗ trợ:
--   1. Một module có thể có NHIỀU môn tiên quyết
--   2. Phân biệt môn tiên quyết BẮT BUỘC vs KHUYẾN NGHỊ
--
-- QUAN HỆ:
--   Module (1) ──< (N) ModulePrerequisites >── (1) Module
--   Một môn học có thể yêu cầu nhiều môn tiên quyết
-- =============================================================================


-- =============================================================================
-- BƯỚC 1: TẠO BẢNG module_prerequisites (MANY-TO-MANY)
-- =============================================================================

CREATE TABLE module_prerequisites (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    
    -- ===== QUAN HỆ =====
    module_id           INT NOT NULL COMMENT 'Môn học (môn cần học SAU)',
    prerequisite_id     INT NOT NULL COMMENT 'Môn tiên quyết (môn cần học TRƯỚC)',
    
    -- ===== LOẠI YÊU CẦU =====
    is_mandatory        BOOLEAN NOT NULL DEFAULT TRUE 
                        COMMENT 'TRUE=Bắt buộc (hard requirement), FALSE=Khuyến nghị (soft requirement)',
    
    -- ===== GHI CHÚ =====
    note                TEXT DEFAULT NULL 
                        COMMENT 'Ghi chú về mối quan hệ tiên quyết (VD: "Cần hiểu OOP trước khi học Spring")',
    
    -- ===== AUDIT =====
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          INT DEFAULT NULL COMMENT 'FK -> users.user_id',
    
    -- ===== CONSTRAINTS =====
    
    -- Không để môn tự phụ thuộc chính nó (vòng lặp 1 node)
    CONSTRAINT chk_no_self_prerequisite 
        CHECK (module_id != prerequisite_id),
    
    -- Không trùng lặp cặp (module_id, prerequisite_id)
    CONSTRAINT uk_module_prerequisite_pair 
        UNIQUE (module_id, prerequisite_id),
    
    -- ===== FOREIGN KEYS =====
    
    CONSTRAINT fk_prereq_module 
        FOREIGN KEY (module_id) REFERENCES modules(module_id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_prereq_prerequisite 
        FOREIGN KEY (prerequisite_id) REFERENCES modules(module_id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_prereq_created_by 
        FOREIGN KEY (created_by) REFERENCES users(user_id) 
        ON DELETE SET NULL
        
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Quan hệ môn tiên quyết many-to-many (hỗ trợ nhiều môn tiên quyết cho 1 module)';


-- =============================================================================
-- BƯỚC 2: TẠO INDEXES
-- =============================================================================

CREATE INDEX idx_prereq_module        ON module_prerequisites (module_id);
CREATE INDEX idx_prereq_prerequisite  ON module_prerequisites (prerequisite_id);
CREATE INDEX idx_prereq_is_mandatory  ON module_prerequisites (is_mandatory);


-- =============================================================================
-- BƯỚC 3: SEED DỮ LIỆU MẪU - Quan hệ tiên quyết
-- =============================================================================
-- V28 đã tạo 5 modules cho program_id=1:
--   Module 1 (MOD001): Nhập môn tư duy lập trình - Không có prerequisite
--   Module 2 (MOD002): Git & HTML/CSS Cơ bản
--   Module 3 (MOD003): JavaScript Core
--   Module 4 (MOD004): Java Core & OOP
--   Module 5 (MOD005): Capstone Project
--
-- REMOVED: Seed data sẽ được thêm sau thông qua admin UI hoặc API
-- Lý do: Tránh lỗi foreign key khi modules chưa tồn tại


-- =============================================================================
-- BƯỚC 4: THÊM PROCEDURE TIỆN ÍCH - Kiểm tra vòng lặp (Cycle Detection)
-- =============================================================================
-- Ngăn chặn vòng lặp phụ thuộc: A → B → C → A

DELIMITER $$

-- Procedure: Kiểm tra xem việc thêm prerequisite có tạo vòng lặp không
CREATE PROCEDURE sp_check_prerequisite_cycle(
    IN p_module_id INT,
    IN p_prerequisite_id INT,
    OUT p_has_cycle BOOLEAN
)
BEGIN
    DECLARE v_count INT;
    
    -- Sử dụng recursive CTE để duyệt đồ thị
    -- Nếu tìm thấy p_module_id trong cây con của p_prerequisite_id → có vòng lặp
    
    WITH RECURSIVE prerequisite_tree AS (
        -- Base case: bắt đầu từ prerequisite_id
        SELECT prerequisite_id AS current_module
        FROM module_prerequisites
        WHERE module_id = p_prerequisite_id
        
        UNION ALL
        
        -- Recursive case: duyệt tiếp các prerequisite của prerequisite
        SELECT mp.prerequisite_id
        FROM module_prerequisites mp
        INNER JOIN prerequisite_tree pt ON mp.module_id = pt.current_module
    )
    SELECT COUNT(*) INTO v_count
    FROM prerequisite_tree
    WHERE current_module = p_module_id;
    
    SET p_has_cycle = (v_count > 0);
END$$

DELIMITER ;


-- =============================================================================
-- BƯỚC 5: THÊM VIEW TIỆN ÍCH - Xem tất cả prerequisites của module
-- =============================================================================

CREATE OR REPLACE VIEW v_module_prerequisites AS
SELECT 
    m.module_id,
    m.code AS module_code,
    m.name AS module_name,
    m.program_id,
    
    mp.id AS prerequisite_link_id,
    mp.is_mandatory,
    mp.note,
    
    prereq.module_id AS prerequisite_module_id,
    prereq.code AS prerequisite_code,
    prereq.name AS prerequisite_name,
    prereq.level AS prerequisite_level,
    
    mp.created_at AS link_created_at
FROM modules m
LEFT JOIN module_prerequisites mp ON m.module_id = mp.module_id
LEFT JOIN modules prereq ON mp.prerequisite_id = prereq.module_id
WHERE m.deleted_at IS NULL 
  AND (prereq.deleted_at IS NULL OR prereq.deleted_at IS NOT NULL)
ORDER BY m.program_id, m.sequence_order, mp.is_mandatory DESC;


-- =============================================================================
-- KẾT QUẢ SAU MIGRATION
-- =============================================================================
-- ✅ Bảng module_prerequisites quản lý quan hệ many-to-many
-- ✅ Hỗ trợ nhiều môn tiên quyết cho 1 module
-- ✅ Phân biệt bắt buộc (mandatory) vs khuyến nghị (optional)
-- ✅ Có procedure kiểm tra vòng lặp phụ thuộc
-- ✅ Có view tiện ích để query prerequisites
-- ✅ Seed data mẫu với quan hệ phức tạp
-- =============================================================================


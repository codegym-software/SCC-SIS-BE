-- =============================================================================
-- V30: Cập nhật logic tính semester cho modules
-- =============================================================================
-- MỤC ĐÍCH:
--   Cập nhật semester cho tất cả modules dựa trên quy tắc mới:
--   - Mỗi semester chứa 6 modules
--   - semester = CEIL(sequence_order / 6)
--   - Ví dụ: Module 1-6 → semester 1, Module 7-12 → semester 2, v.v.
-- =============================================================================


-- =============================================================================
-- Cập nhật semester cho tất cả modules
-- =============================================================================

UPDATE modules 
SET semester = CEILING(sequence_order / 6.0)
WHERE deleted_at IS NULL;


-- =============================================================================
-- KẾT QUẢ SAU MIGRATION
-- =============================================================================
-- ✅ Tất cả modules có semester được tính theo công thức: CEIL(sequence_order / 6)
-- ✅ Module 1-6   → semester 1
-- ✅ Module 7-12  → semester 2
-- ✅ Module 13-18 → semester 3
-- ✅ v.v...
-- =============================================================================


-- =============================================================================
-- V35: Drop Module Prerequisites Table
-- =============================================================================
-- MỤC ĐÍCH:
--   Xóa bảng module_prerequisites vì logic sắp xếp module giờ đơn giản hơn:
--   - Sắp xếp dựa trên sequence_order
--   - Kiểm tra is_mandatory: module bắt buộc không được phép sắp xếp
--   - Module không bắt buộc thì mới được phép sắp xếp
-- =============================================================================

-- Xóa view liên quan đến module_prerequisites
DROP VIEW IF EXISTS v_module_prerequisites;

-- Xóa procedure kiểm tra prerequisite cycle
DROP PROCEDURE IF EXISTS sp_check_prerequisite_cycle;

-- Xóa bảng module_prerequisites và toàn bộ data
DROP TABLE IF EXISTS module_prerequisites;

-- =============================================================================
-- KẾT QUẢ SAU MIGRATION
-- =============================================================================
-- ✅ Bảng module_prerequisites đã được xóa hoàn toàn
-- ✅ View và procedure liên quan đã được xóa
-- ✅ Logic sắp xếp module bây giờ dựa trên:
--    - sequence_order (sắp xếp cơ bản)
--    - is_mandatory (module bắt buộc không được sắp xếp)
--    - Module không bắt buộc mới được phép drag & drop
-- =============================================================================


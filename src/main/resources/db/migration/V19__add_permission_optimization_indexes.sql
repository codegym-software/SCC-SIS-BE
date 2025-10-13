-- V18: Thêm index tối ưu cho API permissions/groups

-- Composite index cho tìm kiếm fuzzy trên name và code với active filter
-- Hỗ trợ query: WHERE active = true AND (name LIKE '%q%' OR code LIKE '%q%')
CREATE INDEX idx_permissions_search_active_name_code ON permissions(active, name, code);

-- Composite index cho group by category với active filter
-- Hỗ trợ query: WHERE active = true GROUP BY category
CREATE INDEX idx_permissions_group_active_category ON permissions(active, category);

-- Note: Các index hiện có đã đủ tốt cho hầu hết trường hợp sử dụng
-- Chỉ thêm index khi thực sự cần thiết và sau khi đo performance
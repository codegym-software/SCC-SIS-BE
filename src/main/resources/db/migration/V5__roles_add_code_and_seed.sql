-- 1) Thêm cột code (cho phép NULL tạm thời để backfill)
ALTER TABLE roles
    ADD COLUMN code VARCHAR(64) NULL AFTER role_id;

-- 2) Backfill code cho các role đang có
-- 2.1 Map tường minh theo tên đang có trong DB của bạn
UPDATE roles SET code = 'SUPER_ADMIN'        WHERE name = 'Super Admin';
UPDATE roles SET code = 'ACADEMIC_STAFF'     WHERE name = 'Giáo vụ';
UPDATE roles SET code = 'LECTURER'           WHERE name = 'Giảng viên';
UPDATE roles SET code = 'CENTER_MANAGER'     WHERE name = 'Quản lý Trung tâm';
UPDATE roles SET code = 'TRAINING_MANAGER'   WHERE name = 'Quản lý Đào tạo';

-- (Nếu còn các role khác, bổ sung thêm UPDATE tương tự ở đây)
-- (Nếu có trùng tên / sai chính tả, sửa lại name trước hoặc sửa WHERE cho đúng)

-- 3) Khóa ràng buộc sau khi đã có dữ liệu code
-- 3.1 Đảm bảo không còn NULL
-- (Chỉ chạy SET NOT NULL nếu tất cả bản ghi đã có code; bạn có thể thêm 1 câu SELECT check ở dưới)
ALTER TABLE roles
    MODIFY COLUMN code VARCHAR(64) NOT NULL;

-- 3.2 Thêm UNIQUE + index cho code
CREATE UNIQUE INDEX uk_roles_code ON roles(code);
CREATE INDEX idx_roles_code ON roles(code);

-- (Tuỳ chọn) vẫn giữ unique trên name như hiện tại để tránh trùng tên hiển thị
-- Nếu trước đây CHƯA có unique name thì có thể thêm:
-- CREATE UNIQUE INDEX uk_roles_name ON roles(name);

-- 4) Kiểm tra nhanh (copy chạy tay sau migration nếu cần)
-- SELECT role_id, code, name FROM roles ORDER BY role_id;

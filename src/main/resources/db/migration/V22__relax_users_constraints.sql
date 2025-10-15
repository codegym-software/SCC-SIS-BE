-- Migration V22: Fix lỗi 409 khi auto-provision user
-- Sửa cột phone thành cho phép NULL để tránh lỗi khi JWT token không có phone

ALTER TABLE users MODIFY phone VARCHAR(32) NULL;

-- Thêm comment để giải thích thay đổi
-- Note: Cột phone giờ đây cho phép NULL để hỗ trợ auto-provision user từ JWT token
-- Khi tạo user từ Keycloak, nếu không có phone claim thì sẽ để NULL thay vì gây lỗi 409
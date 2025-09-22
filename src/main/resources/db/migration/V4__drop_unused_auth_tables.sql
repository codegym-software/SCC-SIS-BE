-- Do sử dụng Keycloak nên xóa 2 bảng sau:
-- Xóa bảng reset password (OTP)
DROP TABLE IF EXISTS password_reset_tokens;

-- Xóa bảng quản lý refresh/session token
DROP TABLE IF EXISTS auth_tokens;

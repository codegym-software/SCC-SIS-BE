-- V11: Thêm assigned_by và revoked_by vào bảng user_roles

ALTER TABLE user_roles 
ADD COLUMN assigned_by VARCHAR(255) COMMENT 'Keycloak ID của người gán role',
ADD COLUMN revoked_by VARCHAR(255) COMMENT 'Keycloak ID của người thu hồi role';
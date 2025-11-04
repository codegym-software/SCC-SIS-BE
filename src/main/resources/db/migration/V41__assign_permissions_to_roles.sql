-- V41: Gán permissions cho các roles trong hệ thống
-- Gán permissions phù hợp cho: SUPER_ADMIN, TRAINING_MANAGER, CENTER_MANAGER, ACADEMIC_STAFF, LECTURER
-- KHÔNG gán permissions cho STUDENT (như yêu cầu)

-- ==================== SUPER_ADMIN ====================
-- Super Admin có TẤT CẢ permissions (full system access)

INSERT INTO role_permissions (role_id, permission_id, granted_at)
SELECT 
    r.role_id,
    p.permission_id,
    NOW() AS granted_at
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN'
  AND r.is_active = TRUE
  AND p.active = TRUE
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp 
    WHERE rp.role_id = r.role_id 
    AND rp.permission_id = p.permission_id
  );

-- ==================== TRAINING_MANAGER ====================
-- Quản lý Đào tạo: Xem user, center, role, permission; Cấu hình hệ thống

INSERT INTO role_permissions (role_id, permission_id, granted_at)
SELECT 
    r.role_id,
    p.permission_id,
    NOW() AS granted_at
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'TRAINING_MANAGER'
  AND r.is_active = TRUE
  AND p.active = TRUE
  AND p.code IN (
    'USER_READ',
    'CENTER_READ',
    'ROLE_READ',
    'PERMISSION_READ',
    'SYSTEM_CONFIG'
  )
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp 
    WHERE rp.role_id = r.role_id 
    AND rp.permission_id = p.permission_id
  );

-- ==================== CENTER_MANAGER ====================
-- Quản lý Trung tâm: Quản lý user, center, gán role cho user

INSERT INTO role_permissions (role_id, permission_id, granted_at)
SELECT 
    r.role_id,
    p.permission_id,
    NOW() AS granted_at
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'CENTER_MANAGER'
  AND r.is_active = TRUE
  AND p.active = TRUE
  AND p.code IN (
    'USER_READ',
    'USER_CREATE',
    'USER_UPDATE',
    'CENTER_READ',
    'CENTER_UPDATE',
    'CENTER_MANAGE',
    'ROLE_READ',
    'ROLE_ASSIGN',
    'ROLE_REVOKE'
  )
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp 
    WHERE rp.role_id = r.role_id 
    AND rp.permission_id = p.permission_id
  );

-- ==================== ACADEMIC_STAFF ====================
-- Giáo vụ: Xem thông tin user, center, role (để quản lý học viên, lớp học, đăng ký)

INSERT INTO role_permissions (role_id, permission_id, granted_at)
SELECT 
    r.role_id,
    p.permission_id,
    NOW() AS granted_at
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ACADEMIC_STAFF'
  AND r.is_active = TRUE
  AND p.active = TRUE
  AND p.code IN (
    'USER_READ',
    'CENTER_READ',
    'ROLE_READ'
  )
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp 
    WHERE rp.role_id = r.role_id 
    AND rp.permission_id = p.permission_id
  );

-- ==================== LECTURER ====================
-- Giảng viên: Xem thông tin user, center (để quản lý lớp học được phân công)

INSERT INTO role_permissions (role_id, permission_id, granted_at)
SELECT 
    r.role_id,
    p.permission_id,
    NOW() AS granted_at
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'LECTURER'
  AND r.is_active = TRUE
  AND p.active = TRUE
  AND p.code IN (
    'USER_READ',
    'CENTER_READ'
  )
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp 
    WHERE rp.role_id = r.role_id 
    AND rp.permission_id = p.permission_id
  );

-- ==================== STUDENT ====================
-- KHÔNG gán permissions cho STUDENT (theo yêu cầu)

-- Kiểm tra kết quả (uncomment để chạy sau khi migration)
-- SELECT 
--     r.code AS role_code,
--     r.name AS role_name,
--     COUNT(rp.permission_id) AS permission_count
-- FROM roles r
-- LEFT JOIN role_permissions rp ON r.role_id = rp.role_id
-- WHERE r.is_active = TRUE
-- GROUP BY r.code, r.name
-- ORDER BY r.code;


-- =========================
-- V7: user_roles - center_id nullable + indexes
-- An toàn cho MySQL < 8.0.29 (không dùng ADD COLUMN IF NOT EXISTS)
-- =========================

-- 0) Biến dùng chung
SET @db := DATABASE();

-- 1) Thêm cột thời gian nếu chưa có: assigned_at
SET @col_exists := (
  SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA=@db AND TABLE_NAME='user_roles' AND COLUMN_NAME='assigned_at'
  LIMIT 1
);
SET @sql := IF(@col_exists IS NULL,
  'ALTER TABLE user_roles ADD COLUMN assigned_at DATETIME NULL',
  'DO 0'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- revoked_at
SET @col_exists := (
  SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA=@db AND TABLE_NAME='user_roles' AND COLUMN_NAME='revoked_at'
  LIMIT 1
);
SET @sql := IF(@col_exists IS NULL,
  'ALTER TABLE user_roles ADD COLUMN revoked_at DATETIME NULL',
  'DO 0'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- created_at
SET @col_exists := (
  SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA=@db AND TABLE_NAME='user_roles' AND COLUMN_NAME='created_at'
  LIMIT 1
);
SET @sql := IF(@col_exists IS NULL,
  'ALTER TABLE user_roles ADD COLUMN created_at DATETIME NULL',
  'DO 0'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 2) Cho phép center_id = NULL
-- LƯU Ý: nếu kiểu cột ở DB của bạn là BIGINT/UNSIGNED, sửa INT cho khớp.
ALTER TABLE user_roles
    MODIFY COLUMN center_id INT NULL;

-- 3) Drop UNIQUE (user_id, role_id, center_id) nếu đang tồn tại
SET @idx := (
  SELECT INDEX_NAME
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA=@db AND TABLE_NAME='user_roles'
    AND INDEX_NAME='uk_user_roles_user_role_center'
  LIMIT 1
);
SET @sql := IF(@idx IS NOT NULL,
  'ALTER TABLE user_roles DROP INDEX `uk_user_roles_user_role_center`',
  'DO 0'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 4) Tạo index nếu chưa có

-- idx_user_roles_user_id (user_id)
SET @exists := (
  SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA=@db AND TABLE_NAME='user_roles'
    AND INDEX_NAME='idx_user_roles_user_id'
  LIMIT 1
);
SET @sql := IF(@exists IS NULL,
  'CREATE INDEX `idx_user_roles_user_id` ON `user_roles` (`user_id`)',
  'DO 0'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- idx_user_roles_center_id (center_id)
SET @exists := (
  SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA=@db AND TABLE_NAME='user_roles'
    AND INDEX_NAME='idx_user_roles_center_id'
  LIMIT 1
);
SET @sql := IF(@exists IS NULL,
  'CREATE INDEX `idx_user_roles_center_id` ON `user_roles` (`center_id`)',
  'DO 0'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- idx_user_roles_user_revoked (user_id, revoked_at)
SET @exists := (
  SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA=@db AND TABLE_NAME='user_roles'
    AND INDEX_NAME='idx_user_roles_user_revoked'
  LIMIT 1
);
SET @sql := IF(@exists IS NULL,
  'CREATE INDEX `idx_user_roles_user_revoked` ON `user_roles` (`user_id`, `revoked_at`)',
  'DO 0'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- idx_user_roles_center_revoked (center_id, revoked_at)
SET @exists := (
  SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA=@db AND TABLE_NAME='user_roles'
    AND INDEX_NAME='idx_user_roles_center_revoked'
  LIMIT 1
);
SET @sql := IF(@exists IS NULL,
  'CREATE INDEX `idx_user_roles_center_revoked` ON `user_roles` (`center_id`, `revoked_at`)',
  'DO 0'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

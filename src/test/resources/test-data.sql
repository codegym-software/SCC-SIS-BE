-- Test Data for Role, Permission, UserRole Tests

-- Insert test roles (H2 compatible single-row inserts with correct column names)
INSERT INTO roles (role_id, code, name, is_active, created_at, updated_at) VALUES (1, 'SUPER_ADMIN', 'Super Administrator', true, NOW(), NOW());
INSERT INTO roles (role_id, code, name, is_active, created_at, updated_at) VALUES (2, 'CENTER_ADMIN', 'Center Administrator', true, NOW(), NOW());
INSERT INTO roles (role_id, code, name, is_active, created_at, updated_at) VALUES (3, 'TEACHER', 'Teacher', true, NOW(), NOW());
INSERT INTO roles (role_id, code, name, is_active, created_at, updated_at) VALUES (4, 'STUDENT', 'Student', true, NOW(), NOW());
INSERT INTO roles (role_id, code, name, is_active, created_at, updated_at) VALUES (5, 'INACTIVE_ROLE', 'Inactive Role', false, NOW(), NOW());

-- Insert test permissions
INSERT INTO permissions (permission_id, code, name, category, active, created_at, updated_at) VALUES (1, 'READ_STUDENT', 'View Students', 'STUDENT', true, NOW(), NOW());
INSERT INTO permissions (permission_id, code, name, category, active, created_at, updated_at) VALUES (2, 'CREATE_STUDENT', 'Create Student', 'STUDENT', true, NOW(), NOW());
INSERT INTO permissions (permission_id, code, name, category, active, created_at, updated_at) VALUES (3, 'UPDATE_STUDENT', 'Update Student', 'STUDENT', true, NOW(), NOW());
INSERT INTO permissions (permission_id, code, name, category, active, created_at, updated_at) VALUES (4, 'DELETE_STUDENT', 'Delete Student', 'STUDENT', true, NOW(), NOW());
INSERT INTO permissions (permission_id, code, name, category, active, created_at, updated_at) VALUES (5, 'READ_ROLE', 'View Roles', 'ROLE', true, NOW(), NOW());
INSERT INTO permissions (permission_id, code, name, category, active, created_at, updated_at) VALUES (6, 'CREATE_ROLE', 'Create Role', 'ROLE', true, NOW(), NOW());
INSERT INTO permissions (permission_id, code, name, category, active, created_at, updated_at) VALUES (7, 'UPDATE_ROLE', 'Update Role', 'ROLE', true, NOW(), NOW());
INSERT INTO permissions (permission_id, code, name, category, active, created_at, updated_at) VALUES (8, 'DELETE_ROLE', 'Delete Role', 'ROLE', true, NOW(), NOW());
INSERT INTO permissions (permission_id, code, name, category, active, created_at, updated_at) VALUES (9, 'READ_PERMISSION', 'View Permissions', 'PERMISSION', true, NOW(), NOW());
INSERT INTO permissions (permission_id, code, name, category, active, created_at, updated_at) VALUES (10, 'ASSIGN_PERMISSION', 'Assign Permissions', 'PERMISSION', true, NOW(), NOW());

-- Insert test centers (Center entity uses deleted_at for soft delete, not active column)
INSERT INTO centers (center_id, name, code, created_at, updated_at) VALUES (1, 'Test Center 1', 'TC001', NOW(), NOW());
INSERT INTO centers (center_id, name, code, created_at, updated_at) VALUES (2, 'Test Center 2', 'TC002', NOW(), NOW());

-- Insert test users (need keycloak_user_id for User entity, is_active column name)
INSERT INTO users (user_id, full_name, email, keycloak_user_id, is_active, created_at, updated_at) VALUES (1, 'Super Admin User', 'superadmin@test.com', 'kc-superadmin', true, NOW(), NOW());
INSERT INTO users (user_id, full_name, email, keycloak_user_id, is_active, created_at, updated_at) VALUES (2, 'Center Admin User', 'centeradmin@test.com', 'kc-centeradmin', true, NOW(), NOW());
INSERT INTO users (user_id, full_name, email, keycloak_user_id, is_active, created_at, updated_at) VALUES (3, 'Teacher One', 'teacher01@test.com', 'kc-teacher01', true, NOW(), NOW());
INSERT INTO users (user_id, full_name, email, keycloak_user_id, is_active, created_at, updated_at) VALUES (4, 'Student One', 'student01@test.com', 'kc-student01', true, NOW(), NOW());
INSERT INTO users (user_id, full_name, email, keycloak_user_id, is_active, created_at, updated_at) VALUES (5, 'Test User', 'testuser@test.com', 'kc-testuser', true, NOW(), NOW());

-- Insert mock test users for integration tests (matching JWT mock tokens)
INSERT INTO users (user_id, full_name, email, keycloak_user_id, is_active, created_at, updated_at) VALUES (100, 'Mock Super Admin', 'mock-sa@test.com', 'test-user', true, NOW(), NOW());
INSERT INTO users (user_id, full_name, email, keycloak_user_id, is_active, created_at, updated_at) VALUES (101, 'Mock Teacher', 'mock-teacher@test.com', 'test-teacher', true, NOW(), NOW());
INSERT INTO users (user_id, full_name, email, keycloak_user_id, is_active, created_at, updated_at) VALUES (102, 'Mock Center Admin', 'mock-centeradmin@test.com', 'test-centeradmin', true, NOW(), NOW());

-- Insert role-permission mappings
INSERT INTO role_permissions (role_permission_id, role_id, permission_id, granted_at, granted_by) VALUES (1, 1, 1, NOW(), 'system');
INSERT INTO role_permissions (role_permission_id, role_id, permission_id, granted_at, granted_by) VALUES (2, 1, 2, NOW(), 'system');
INSERT INTO role_permissions (role_permission_id, role_id, permission_id, granted_at, granted_by) VALUES (3, 1, 3, NOW(), 'system');
INSERT INTO role_permissions (role_permission_id, role_id, permission_id, granted_at, granted_by) VALUES (4, 1, 4, NOW(), 'system');
INSERT INTO role_permissions (role_permission_id, role_id, permission_id, granted_at, granted_by) VALUES (5, 1, 5, NOW(), 'system');
INSERT INTO role_permissions (role_permission_id, role_id, permission_id, granted_at, granted_by) VALUES (6, 1, 6, NOW(), 'system');
INSERT INTO role_permissions (role_permission_id, role_id, permission_id, granted_at, granted_by) VALUES (7, 1, 7, NOW(), 'system');
INSERT INTO role_permissions (role_permission_id, role_id, permission_id, granted_at, granted_by) VALUES (8, 1, 8, NOW(), 'system');
INSERT INTO role_permissions (role_permission_id, role_id, permission_id, granted_at, granted_by) VALUES (9, 1, 9, NOW(), 'system');
INSERT INTO role_permissions (role_permission_id, role_id, permission_id, granted_at, granted_by) VALUES (10, 1, 10, NOW(), 'system');
INSERT INTO role_permissions (role_permission_id, role_id, permission_id, granted_at, granted_by) VALUES (11, 3, 1, NOW(), 'system');
INSERT INTO role_permissions (role_permission_id, role_id, permission_id, granted_at, granted_by) VALUES (12, 3, 2, NOW(), 'system');
INSERT INTO role_permissions (role_permission_id, role_id, permission_id, granted_at, granted_by) VALUES (13, 4, 1, NOW(), 'system');

-- Insert user-role assignments
INSERT INTO user_roles (user_role_id, user_id, role_id, center_id, assigned_at, assigned_by, revoked_at) VALUES (1, 1, 1, NULL, NOW(), 'system', NULL);
INSERT INTO user_roles (user_role_id, user_id, role_id, center_id, assigned_at, assigned_by, revoked_at) VALUES (2, 2, 2, 1, NOW(), 'system', NULL);
INSERT INTO user_roles (user_role_id, user_id, role_id, center_id, assigned_at, assigned_by, revoked_at) VALUES (3, 3, 3, 1, NOW(), 'system', NULL);
INSERT INTO user_roles (user_role_id, user_id, role_id, center_id, assigned_at, assigned_by, revoked_at) VALUES (4, 4, 4, 1, NOW(), 'system', NULL);

-- Insert user-role assignments for mock test users (for integration tests)
INSERT INTO user_roles (user_role_id, user_id, role_id, center_id, assigned_at, assigned_by, revoked_at) VALUES (100, 100, 1, NULL, NOW(), 'system', NULL);  -- Mock Super Admin
INSERT INTO user_roles (user_role_id, user_id, role_id, center_id, assigned_at, assigned_by, revoked_at) VALUES (101, 101, 3, 1, NOW(), 'system', NULL);  -- Mock Teacher
INSERT INTO user_roles (user_role_id, user_id, role_id, center_id, assigned_at, assigned_by, revoked_at) VALUES (102, 102, 2, 1, NOW(), 'system', NULL);  -- Mock Center Admin

package com.example.sis.services;

import com.example.sis.dtos.role.CreateRoleRequest;
import com.example.sis.dtos.role.RoleResponse;
import com.example.sis.dtos.role.UpdateRoleRequest;

import java.util.List;

/**
 * RoleService:
 * - active == null hoặc true => trả về chỉ các role đang active
 * - active == false => trả về tất cả role
 */
public interface RoleService {
    List<RoleResponse> listRoles(Boolean active);

    /**
     * Tạo role mới (chỉ Super Admin)
     */
    RoleResponse createRole(CreateRoleRequest request);

    /**
     * Cập nhật role (chỉ Super Admin)
     */
    RoleResponse updateRole(Integer roleId, UpdateRoleRequest request);

    /**
     * Xóa role (chỉ Super Admin)
     */
    void deleteRole(Integer roleId);

    /**
     * Lấy thông tin role theo ID
     */
    RoleResponse getRoleById(Integer roleId);
}

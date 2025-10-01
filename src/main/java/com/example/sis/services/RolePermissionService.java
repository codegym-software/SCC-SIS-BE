package com.example.sis.services;

import com.example.sis.dtos.permission.PermissionResponse;
import com.example.sis.dtos.rolepermission.RolePermissionRequest;
import com.example.sis.dtos.rolepermission.RolePermissionResponse;

import java.util.List;

/**
 * RolePermissionService:
 * - Quản lý việc gán/thu hồi quyền cho role
 * - Chỉ Super Admin mới có quyền thực hiện
 */
public interface RolePermissionService {

    /**
     * Lấy danh sách permissions của một role
     * 
     * @param roleId ID của role
     * @return danh sách permissions
     */
    List<PermissionResponse> getPermissionsByRoleId(Integer roleId);

    /**
     * Gán permission cho role
     * 
     * @param request   thông tin gán permission
     * @param grantedBy người thực hiện gán (Keycloak ID)
     * @return thông tin permission đã gán
     */
    RolePermissionResponse assignPermissionToRole(RolePermissionRequest request, String grantedBy);

    /**
     * Thu hồi permission từ role
     * 
     * @param roleId       ID của role
     * @param permissionId ID của permission
     */
    void revokePermissionFromRole(Integer roleId, Integer permissionId);

    /**
     * Gán nhiều permissions cho role
     * 
     * @param roleId        ID của role
     * @param permissionIds danh sách ID permissions
     * @param grantedBy     người thực hiện gán (Keycloak ID)
     * @return danh sách permissions đã gán
     */
    List<RolePermissionResponse> assignMultiplePermissionsToRole(Integer roleId, List<Integer> permissionIds,
            String grantedBy);

    /**
     * Lấy danh sách permissions chưa được gán cho role
     * 
     * @param roleId ID của role
     * @return danh sách permissions chưa gán
     */
    List<PermissionResponse> getUnassignedPermissionsByRoleId(Integer roleId);
}
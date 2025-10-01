package com.example.sis.services;

import com.example.sis.dtos.userrole.UserRoleRequest;
import com.example.sis.dtos.userrole.UserRoleResponse;

import java.util.List;

/**
 * UserRoleService:
 * - Quản lý việc gán/thu hồi role cho user tại center
 * - Super Admin có quyền thực hiện ở tất cả center
 * - Center Manager chỉ có quyền thực hiện ở center mình quản lý
 */
public interface UserRoleService {

    /**
     * Gán role cho user tại center cụ thể
     * 
     * @param request    thông tin gán role
     * @param assignedBy người thực hiện gán (Keycloak ID)
     * @return thông tin user-role đã gán
     */
    UserRoleResponse assignRoleToUser(UserRoleRequest request, String assignedBy);

    /**
     * Thu hồi role từ user tại center
     * 
     * @param userRoleId ID của user-role
     * @param revokedBy  người thực hiện thu hồi (Keycloak ID)
     */
    void revokeRoleFromUser(Integer userRoleId, String revokedBy);

    /**
     * Lấy danh sách user-roles của một center
     * 
     * @param centerId ID của center
     * @return danh sách user-roles
     */
    List<UserRoleResponse> getUserRolesByCenterId(Integer centerId);

    /**
     * Lấy danh sách user-roles của một user
     * 
     * @param userId ID của user
     * @return danh sách user-roles
     */
    List<UserRoleResponse> getUserRolesByUserId(Integer userId);

    /**
     * Kiểm tra user có role cụ thể tại center không
     * 
     * @param userId   ID của user
     * @param roleCode mã role
     * @param centerId ID của center (có thể null cho role global)
     * @return true nếu user có role tại center
     */
    boolean hasRoleAtCenter(Integer userId, String roleCode, Integer centerId);
}
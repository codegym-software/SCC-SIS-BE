package com.example.sis.services;

import com.example.sis.dtos.role.RoleResponse;

import java.util.List;

/**
 * RoleService:
 *  - active == null hoặc true  => trả về chỉ các role đang active
 *  - active == false           => trả về tất cả role
 */
public interface RoleService {
    List<RoleResponse> listRoles(Boolean active);
}

package com.example.sis.services.impl;

import com.example.sis.constants.RoleScope; // enum ở constants
import com.example.sis.dtos.role.CreateRoleRequest;
import com.example.sis.dtos.role.RoleResponse; // DTO ở dtos.role
import com.example.sis.dtos.role.UpdateRoleRequest;
import com.example.sis.models.Role; // entity ở models
import com.example.sis.repositories.RoleRepository; // repo ở repositories
import com.example.sis.services.RoleService; // interface ở services
import com.example.sis.utils.RoleScopeUtil; // util bạn đã có

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public List<RoleResponse> listRoles(Boolean active) {
        final List<Role> roles = (active == null || Boolean.TRUE.equals(active))
                ? roleRepository.findByActiveTrueOrderByNameAsc()
                : roleRepository.findAllByOrderByNameAsc();

        return roles.stream()
                .map(r -> new RoleResponse(
                        r.getRoleId(),
                        r.getCode(),
                        r.getName(),
                        resolveScope(r.getCode()),
                        r.isActive()))
                .collect(Collectors.toList());
    }

    /**
     * Xác định scope dựa theo util của bạn:
     * - isExclusiveGlobal(code) -> GLOBAL
     * - isCenterScoped(code) -> CENTER
     * - fallback -> CENTER
     */
    private RoleScope resolveScope(String code) {
        if (code == null)
            return RoleScope.CENTER;
        if (RoleScopeUtil.isExclusiveGlobal(code)) {
            return RoleScope.GLOBAL;
        }
        if (RoleScopeUtil.isCenterScoped(code)) {
            return RoleScope.CENTER;
        }
        return RoleScope.CENTER;
    }

    @Override
    public RoleResponse createRole(CreateRoleRequest request) {
        // Kiểm tra mã role đã tồn tại chưa
        if (roleRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Mã role đã tồn tại: " + request.getCode());
        }

        final Role role = new Role();
        role.setCode(request.getCode());
        role.setName(request.getName());
        role.setActive(request.getActive() != null ? request.getActive() : true);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());

        final Role saved = roleRepository.save(role);
        return convertToResponse(saved);
    }

    @Override
    public RoleResponse updateRole(Integer roleId, UpdateRoleRequest request) {
        final Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại với ID: " + roleId));

        role.setName(request.getName());
        role.setUpdatedAt(LocalDateTime.now());
        if (request.getActive() != null) {
            role.setActive(request.getActive());
        }

        final Role saved = roleRepository.save(role);
        return convertToResponse(saved);
    }

    @Override
    public void deleteRole(Integer roleId) {
        final Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại với ID: " + roleId));

        // Soft delete - chỉ đánh dấu không active
        role.setActive(false);
        role.setUpdatedAt(LocalDateTime.now());
        roleRepository.save(role);
    }

    @Override
    public RoleResponse getRoleById(Integer roleId) {
        final Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại với ID: " + roleId));

        return convertToResponse(role);
    }

    /**
     * Convert Role entity to RoleResponse DTO
     */
    private RoleResponse convertToResponse(Role role) {
        return new RoleResponse(
                role.getRoleId(),
                role.getCode(),
                role.getName(),
                resolveScope(role.getCode()),
                role.isActive());
    }
}

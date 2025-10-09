package com.example.sis.services.impl;

import com.example.sis.enums.RoleScope; // <— đường dẫn đúng
import com.example.sis.dtos.role.CreateRoleRequest;
import com.example.sis.dtos.role.RoleResponse;
import com.example.sis.dtos.role.UpdateRoleRequest;
import com.example.sis.models.Role;
import com.example.sis.models.Permission;
import com.example.sis.models.RolePermission;
import com.example.sis.repositories.RoleRepository;
import com.example.sis.repositories.PermissionRepository;
import com.example.sis.repositories.RolePermissionRepository;
import com.example.sis.services.RoleService;
import com.example.sis.utils.RoleScopeUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public RoleServiceImpl(RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles(Boolean active) {
        // Repo giờ trả Page => dùng Pageable.unpaged() để giữ nguyên chữ ký List<>
        Page<Role> page = (active == null || Boolean.TRUE.equals(active))
                ? roleRepository.findByActiveTrueOrderByNameAsc(Pageable.unpaged())
                : roleRepository.findAllByOrderByNameAsc(Pageable.unpaged());

        return page.getContent().stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public RoleResponse createRole(CreateRoleRequest request) {
        if (roleRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Mã role đã tồn tại: " + request.getCode());
        }

        Role role = new Role();
        role.setCode(request.getCode());
        role.setName(request.getName());
        role.setActive(request.getActive() != null ? request.getActive() : true);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());

        Role saved = roleRepository.save(role);

        // Gán quyền cho role nếu có permissionIds trong request
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            // Validate tất cả permissions tồn tại
            List<Permission> permissions = permissionRepository.findAllById(request.getPermissionIds());
            if (permissions.size() != request.getPermissionIds().size()) {
                throw new RuntimeException("Một hoặc nhiều permission ID không tồn tại");
            }

            // Tạo role_permissions records
            for (Permission permission : permissions) {
                RolePermission rolePermission = new RolePermission(saved, permission, null); // grantedBy null vì tạo từ
                                                                                             // API
                rolePermissionRepository.save(rolePermission);
            }
        }

        return convertToResponse(saved);
    }

    @Override
    public RoleResponse updateRole(Integer roleId, UpdateRoleRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại với ID: " + roleId));

        role.setName(request.getName());
        if (request.getActive() != null) {
            role.setActive(request.getActive());
        }
        role.setUpdatedAt(LocalDateTime.now());

        Role saved = roleRepository.save(role);
        return convertToResponse(saved);
    }

    @Override
    public void deleteRole(Integer roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại với ID: " + roleId));

        // Soft delete
        role.setActive(false);
        role.setUpdatedAt(LocalDateTime.now());
        roleRepository.save(role);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(Integer roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại với ID: " + roleId));
        return convertToResponse(role);
    }

    // === Helpers ===
    private RoleResponse convertToResponse(Role role) {
        return new RoleResponse(
                role.getRoleId(),
                role.getCode(),
                role.getName(),
                resolveScope(role.getCode()),
                role.isActive());
    }

    private RoleScope resolveScope(String code) {
        if (code == null)
            return RoleScope.CENTER;
        if (RoleScopeUtil.isExclusiveGlobal(code))
            return RoleScope.GLOBAL;
        if (RoleScopeUtil.isCenterScoped(code))
            return RoleScope.CENTER;
        return RoleScope.CENTER;
    }
}

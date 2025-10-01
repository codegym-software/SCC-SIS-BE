package com.example.sis.services.impl;

import com.example.sis.dtos.permission.PermissionResponse;
import com.example.sis.dtos.role.RoleResponse;
import com.example.sis.constants.RoleScope;
import com.example.sis.dtos.rolepermission.RolePermissionRequest;
import com.example.sis.dtos.rolepermission.RolePermissionResponse;
import com.example.sis.models.Permission;
import com.example.sis.models.Role;
import com.example.sis.models.RolePermission;
import com.example.sis.repositories.PermissionRepository;
import com.example.sis.repositories.RoleRepository;
import com.example.sis.repositories.RolePermissionRepository;
import com.example.sis.services.RolePermissionService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RolePermissionServiceImpl implements RolePermissionService {

    private final RolePermissionRepository rolePermissionRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RolePermissionServiceImpl(RolePermissionRepository rolePermissionRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository) {
        this.rolePermissionRepository = rolePermissionRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public List<PermissionResponse> getPermissionsByRoleId(Integer roleId) {
        final List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleIdWithPermissions(roleId);

        return rolePermissions.stream()
                .map(rp -> convertPermissionToResponse(rp.getPermission()))
                .collect(Collectors.toList());
    }

    @Override
    public RolePermissionResponse assignPermissionToRole(RolePermissionRequest request, String grantedBy) {
        final Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role không tồn tại với ID: " + request.getRoleId()));

        final Permission permission = permissionRepository.findById(request.getPermissionId())
                .orElseThrow(
                        () -> new RuntimeException("Permission không tồn tại với ID: " + request.getPermissionId()));

        // Kiểm tra đã gán chưa
        if (rolePermissionRepository.existsByRoleAndPermission(role, permission)) {
            throw new RuntimeException("Permission đã được gán cho Role này");
        }

        final RolePermission rolePermission = new RolePermission(role, permission, grantedBy);
        final RolePermission saved = rolePermissionRepository.save(rolePermission);

        return convertToResponse(saved);
    }

    @Override
    public void revokePermissionFromRole(Integer roleId, Integer permissionId) {
        final Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại với ID: " + roleId));

        final Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("Permission không tồn tại với ID: " + permissionId));

        rolePermissionRepository.deleteByRoleAndPermission(role, permission);
    }

    @Override
    public List<RolePermissionResponse> assignMultiplePermissionsToRole(Integer roleId, List<Integer> permissionIds,
            String grantedBy) {
        final Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại với ID: " + roleId));

        final List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw new RuntimeException("Một số Permission không tồn tại");
        }

        final List<RolePermission> rolePermissions = permissions.stream()
                .filter(permission -> !rolePermissionRepository.existsByRoleAndPermission(role, permission))
                .map(permission -> new RolePermission(role, permission, grantedBy))
                .collect(Collectors.toList());

        final List<RolePermission> saved = rolePermissionRepository.saveAll(rolePermissions);

        return saved.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionResponse> getUnassignedPermissionsByRoleId(Integer roleId) {
        final List<Permission> permissions = rolePermissionRepository.findUnassignedPermissionsByRoleId(roleId);

        return permissions.stream()
                .map(this::convertPermissionToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert RolePermission to RolePermissionResponse
     */
    private RolePermissionResponse convertToResponse(RolePermission rolePermission) {
        return new RolePermissionResponse(
                rolePermission.getRolePermissionId(),
                convertRoleToResponse(rolePermission.getRole()),
                convertPermissionToResponse(rolePermission.getPermission()),
                rolePermission.getGrantedAt(),
                rolePermission.getGrantedBy());
    }

    /**
     * Convert Role to RoleResponse
     */
    private RoleResponse convertRoleToResponse(Role role) {
        return new RoleResponse(
                role.getRoleId(),
                role.getCode(),
                role.getName(),
                resolveScope(role.getCode()),
                role.isActive());
    }

    /**
     * Resolve scope based on role code
     */
    private RoleScope resolveScope(String roleCode) {
        if (roleCode.contains("SUPER")) {
            return RoleScope.GLOBAL;
        } else if (roleCode.contains("CENTER")) {
            return RoleScope.CENTER;
        }
        return RoleScope.CENTER; // default
    }

    /**
     * Convert Permission to PermissionResponse
     */
    private PermissionResponse convertPermissionToResponse(Permission permission) {
        return new PermissionResponse(
                permission.getPermissionId(),
                permission.getCode(),
                permission.getName(),
                permission.getDescription(),
                permission.getCategory(),
                permission.getActive(),
                permission.getCreatedAt(),
                permission.getUpdatedAt());
    }
}
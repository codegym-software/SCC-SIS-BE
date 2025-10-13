package com.example.sis.services.impl;

import com.example.sis.dtos.permission.PermissionResponse;
import com.example.sis.dtos.role.RoleResponse;
import com.example.sis.enums.RoleScope;
import com.example.sis.dtos.rolepermission.RolePermissionRequest;
import com.example.sis.dtos.rolepermission.RolePermissionResponse;
import com.example.sis.exceptions.NotFoundException;
import com.example.sis.models.Permission;
import com.example.sis.models.Role;
import com.example.sis.models.RolePermission;
import com.example.sis.repositories.PermissionRepository;
import com.example.sis.repositories.RolePermissionRepository;
import com.example.sis.repositories.RoleRepository;
import com.example.sis.services.RolePermissionService;
import com.example.sis.utils.RoleScopeUtil;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class RolePermissionServiceImpl implements RolePermissionService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 200;

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

    // ========= List assigned =========
    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> listAssigned(Integer roleId, String q, String category,
                                                 Integer page, Integer size, String sort) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found: id=" + roleId));

        Pageable pageable = toUnsortedPageable(page, size); // avoid spring appending wrong ORDER BY
        Page<Permission> result = rolePermissionRepository.pageAssignedPermissions(
                role.getRoleId(), normalize(q), normalize(category), pageable);

        return result.getContent().stream().map(this::toPermissionDto).toList();
    }

    // ========= List unassigned =========
    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> listUnassigned(Integer roleId, String q, String category,
                                                   Integer page, Integer size, String sort) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found: id=" + roleId));

        Pageable pageable = toUnsortedPageable(page, size); // query already has ORDER BY p.name
        Page<Permission> result = rolePermissionRepository.pageUnassignedPermissions(
                role.getRoleId(), normalize(q), normalize(category), pageable);

        return result.getContent().stream().map(this::toPermissionDto).toList();
    }

    // ========= Assign ONE (idempotent) =========
    @Override
    public RolePermissionResponse assignPermissionToRole(RolePermissionRequest request, String grantedBy) {
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new NotFoundException("Role not found: id=" + request.getRoleId()));
        Permission permission = permissionRepository.findById(request.getPermissionId())
                .orElseThrow(() -> new NotFoundException("Permission not found: id=" + request.getPermissionId()));

        return rolePermissionRepository.findByRoleAndPermission(role, permission)
                .map(this::toRolePermissionDto) // idempotent: return existing
                .orElseGet(() -> {
                    RolePermission created = rolePermissionRepository.save(
                            new RolePermission(role, permission, grantedBy));
                    return toRolePermissionDto(created);
                });
    }

    // ========= Assign MANY (idempotent per item) =========
    @Override
    public List<RolePermissionResponse> assignMultiplePermissionsToRole(Integer roleId, List<Integer> permissionIds,
                                                                        String grantedBy) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found: id=" + roleId));

        if (permissionIds == null || permissionIds.isEmpty()) return List.of();

        // Validate all permissions exist (unique input)
        Set<Integer> uniq = new HashSet<>(permissionIds);
        List<Permission> permissions = permissionRepository.findAllById(uniq);
        if (permissions.size() != uniq.size()) {
            throw new NotFoundException("Some permissions not found");
        }

        // Only create missing links
        List<RolePermission> toCreate = new ArrayList<>();
        for (Permission p : permissions) {
            if (!rolePermissionRepository.existsByRoleAndPermission(role, p)) {
                toCreate.add(new RolePermission(role, p, grantedBy));
            }
        }
        if (toCreate.isEmpty()) return List.of();

        List<RolePermission> saved = rolePermissionRepository.saveAll(toCreate);
        return saved.stream().map(this::toRolePermissionDto).toList();
    }

    // ========= Revoke ONE =========
    @Override
    public void revokePermissionFromRole(Integer roleId, Integer permissionId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found: id=" + roleId));
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new NotFoundException("Permission not found: id=" + permissionId));

        rolePermissionRepository.deleteByRoleAndPermission(role, permission);
    }

    // ========= Revoke MANY (bulk) =========
    @Override
    public void revokeMultiplePermissionsFromRole(Integer roleId, List<Integer> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) return;

        // Ensure role exists (fast fail)
        if (!roleRepository.existsById(roleId)) {
            throw new NotFoundException("Role not found: id=" + roleId);
        }

        // Bulk delete; repo query ignores non-existing pairs gracefully
        rolePermissionRepository.deleteByRoleIdAndPermissionIds(roleId, permissionIds);
    }

    // ========= Helpers =========
    private PermissionResponse toPermissionDto(Permission p) {
        return new PermissionResponse(
                p.getPermissionId(),
                p.getCode(),
                p.getName(),
                p.getDescription(),
                p.getCategory(),
                p.getActive(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    private RolePermissionResponse toRolePermissionDto(RolePermission rp) {
        return new RolePermissionResponse(
                rp.getRolePermissionId(),
                toRoleDto(rp.getRole()),
                toPermissionDto(rp.getPermission()),
                rp.getGrantedAt(),
                rp.getGrantedBy()
        );
    }

    private RoleResponse toRoleDto(Role r) {
        return new RoleResponse(
                r.getRoleId(),
                r.getCode(),
                r.getName(),
                r.isActive(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                0L, // userCount sẽ được tính riêng nếu cần
                Set.of(), // permissionIds sẽ được load riêng nếu cần
                Map.of() // summary sẽ được build riêng nếu cần
        );
    }

    private RoleScope resolveScope(String code) {
        if (code == null) return RoleScope.CENTER;
        if (RoleScopeUtil.isExclusiveGlobal(code)) return RoleScope.GLOBAL;
        if (RoleScopeUtil.isCenterScoped(code))   return RoleScope.CENTER;
        return RoleScope.CENTER;
    }

    private String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // Important: unsorted to avoid spring appending ORDER BY on wrong alias
    private Pageable toUnsortedPageable(Integer page, Integer size) {
        int p = (page == null || page < 0) ? DEFAULT_PAGE : page;
        int s = (size == null || size <= 0) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(p, s, Sort.unsorted());
    }
}

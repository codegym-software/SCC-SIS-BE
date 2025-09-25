package com.example.sis.service;

import com.example.sis.model.Permission;
import com.example.sis.model.Role;
import com.example.sis.repository.PermissionRepository;
import com.example.sis.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public Set<Permission> getPermissionsByRole(Integer roleId) {
        Role role = roleRepository.findWithPermissionsByRoleId(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        return role.getPermissions();
    }

    @Transactional
    public Role assignPermissionsToRole(Integer roleId, List<Integer> permissionIds) {
        Role role = roleRepository.findWithPermissionsByRoleId(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        if (permissions.isEmpty()) {
            throw new RuntimeException("No permissions found for given IDs");
        }

        role.getPermissions().addAll(permissions);
        return roleRepository.save(role);
    }

    @Transactional
    public Role removePermissionFromRole(Integer roleId, Integer permissionId) {
        Role role = roleRepository.findWithPermissionsByRoleId(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        Optional<Permission> permission = permissionRepository.findById(permissionId);
        permission.ifPresent(p -> role.getPermissions().remove(p));

        return roleRepository.save(role);
    }

    @Transactional(readOnly = true)
    public List<Role> getAllRolesWithPermissions() {
        return roleRepository.findAll()
                .stream()
                .peek(r -> r.getPermissions().size()) // force load
                .collect(Collectors.toList());
    }
}

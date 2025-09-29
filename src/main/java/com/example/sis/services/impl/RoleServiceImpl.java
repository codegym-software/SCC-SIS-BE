package com.example.sis.services.impl;

import com.example.sis.constants.RoleScope;              // enum ở constants
import com.example.sis.dtos.role.RoleResponse;          // DTO ở dtos.role
import com.example.sis.models.Role;                     // entity ở models
import com.example.sis.repositories.RoleRepository;     // repo ở repositories
import com.example.sis.services.RoleService;            // interface ở services
import com.example.sis.utils.RoleScopeUtil;             // util bạn đã có

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
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
                        r.isActive()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Xác định scope dựa theo util của bạn:
     *  - isExclusiveGlobal(code)  -> GLOBAL
     *  - isCenterScoped(code)     -> CENTER
     *  - fallback                 -> CENTER
     */
    private RoleScope resolveScope(String code) {
        if (code == null) return RoleScope.CENTER;
        if (RoleScopeUtil.isExclusiveGlobal(code)) {
            return RoleScope.GLOBAL;
        }
        if (RoleScopeUtil.isCenterScoped(code)) {
            return RoleScope.CENTER;
        }
        return RoleScope.CENTER;
    }
}

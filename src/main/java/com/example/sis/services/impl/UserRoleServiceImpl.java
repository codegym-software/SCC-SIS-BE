package com.example.sis.services.impl;

import com.example.sis.dtos.userrole.UserRoleRequest;
import com.example.sis.dtos.userrole.UserRoleResponse;
import com.example.sis.dtos.user.UserResponse;
import com.example.sis.dtos.role.RoleResponse;
import com.example.sis.dtos.center.CenterResponse;
import com.example.sis.exceptions.BadRequestException;
import com.example.sis.exceptions.NotFoundException;
import com.example.sis.models.Center;
import com.example.sis.models.Role;
import com.example.sis.models.User;
import com.example.sis.models.UserRole;
import com.example.sis.repositories.CenterRepository;
import com.example.sis.repositories.RoleRepository;
import com.example.sis.repositories.UserRepository;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.services.UserRoleService;
import com.example.sis.utils.RoleScopeUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserRoleServiceImpl implements UserRoleService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 200;

    // Soft limit for center-scoped roles per user (tune as needed)
    private static final int MAX_ACTIVE_CENTER_ROLES_PER_USER = 3;

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final CenterRepository centerRepository;
    private final UserRepository userRepository;

    public UserRoleServiceImpl(UserRoleRepository userRoleRepository,
                               RoleRepository roleRepository,
                               CenterRepository centerRepository,
                               UserRepository userRepository) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.centerRepository = centerRepository;
        this.userRepository = userRepository;
    }

    /**
     * Assign ONE role to user.
     * - Scope validation (GLOBAL vs CENTER).
     * - GLOBAL-exclusive rule.
     * - Idempotent.
     */
    @Override
    public UserRoleResponse assignRoleToUser(UserRoleRequest request, String assignedBy) {
        if (request.getUserId() == null) throw new BadRequestException("User ID must not be null");
        if (request.getRoleId() == null) throw new BadRequestException("Role ID must not be null");

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found: id=" + request.getUserId()));
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new NotFoundException("Role not found: id=" + request.getRoleId()));

        String roleCode = role.getCode();
        boolean isGlobal = RoleScopeUtil.isExclusiveGlobal(roleCode);
        boolean isCenterScoped = RoleScopeUtil.isCenterScoped(roleCode) || !isGlobal;

        if (isGlobal && request.getCenterId() != null) {
            throw new BadRequestException("Global role must be assigned without centerId");
        }
        if (isCenterScoped && request.getCenterId() == null) {
            throw new BadRequestException("Center-scoped role requires centerId");
        }

        // Load current actives to enforce exclusivity / limits
        List<UserRole> current = userRoleRepository.findActiveByUserId(request.getUserId());
        boolean hasAnyGlobalActive = current.stream()
                .anyMatch(ur -> RoleScopeUtil.isExclusiveGlobal(ur.getRole().getCode()));
        if (hasAnyGlobalActive) {
            throw new BadRequestException("User already has a GLOBAL role; cannot assign additional roles");
        }
        if (isGlobal && !current.isEmpty()) {
            throw new BadRequestException("Cannot assign a GLOBAL role to a user who already has active roles");
        }

        long currentCenterCount = current.stream()
                .filter(ur -> !RoleScopeUtil.isExclusiveGlobal(ur.getRole().getCode()))
                .count();
        if (isCenterScoped && currentCenterCount + 1 > MAX_ACTIVE_CENTER_ROLES_PER_USER) {
            throw new BadRequestException("Exceeds max center roles per user (" + MAX_ACTIVE_CENTER_ROLES_PER_USER + ")");
        }

        // Idempotent
        boolean exists = userRoleRepository.existsActiveAssignment(
                request.getUserId(), request.getRoleId(), request.getCenterId());
        if (exists) {
            return current.stream()
                    .filter(ur ->
                            Objects.equals(ur.getRole().getRoleId(), request.getRoleId()) &&
                                    Objects.equals(ur.getCenter() != null ? ur.getCenter().getCenterId() : null,
                                            request.getCenterId()))
                    .findFirst()
                    .map(this::toDto)
                    .orElseThrow(() -> new NotFoundException("Active assignment not found though existence check passed"));
        }

        Center center = null;
        if (request.getCenterId() != null) {
            center = centerRepository.findById(request.getCenterId())
                    .orElseThrow(() -> new NotFoundException("Center not found: id=" + request.getCenterId()));
        }

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setCenter(center);
        userRole.setAssignedBy(assignedBy);
        userRole.setAssignedAt(LocalDateTime.now());
        userRole.setCreatedAt(LocalDateTime.now());

        userRole = userRoleRepository.save(userRole);
        return toDto(userRole);
    }

    /**
     * Assign MANY roles to user (idempotent per item).
     * - All items must target same user.
     * - Scope validation per item.
     * - GLOBAL-exclusive rule & center limit applied.
     */
    @Override
    public List<UserRoleResponse> assignRolesToUser(Integer userId, List<UserRoleRequest> requests, String assignedBy) {
        if (userId == null) throw new BadRequestException("User ID must not be null");
        if (requests == null || requests.isEmpty()) return List.of();

        // Normalize: set userId for all items; ensure same user
        for (UserRoleRequest r : requests) {
            if (r == null) throw new BadRequestException("Invalid item");
            if (r.getRoleId() == null) throw new BadRequestException("Role ID must not be null");
            if (r.getUserId() == null) r.setUserId(userId);
            if (!Objects.equals(r.getUserId(), userId)) {
                throw new BadRequestException("All items must target the same user");
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: id=" + userId));

        // Current actives & exclusivity pre-check
        List<UserRole> current = userRoleRepository.findActiveByUserId(userId);
        boolean hasAnyGlobalActive = current.stream()
                .anyMatch(ur -> RoleScopeUtil.isExclusiveGlobal(ur.getRole().getCode()));
        if (hasAnyGlobalActive) {
            throw new BadRequestException("User already has a GLOBAL role; cannot assign additional roles");
        }

        // De-duplicate incoming (roleId, centerId)
        class Key {
            final Integer roleId;
            final Integer centerId;
            Key(Integer roleId, Integer centerId) { this.roleId = roleId; this.centerId = centerId; }
            @Override public boolean equals(Object o) {
                if (this == o) return true; if (o == null || getClass() != o.getClass()) return false;
                Key key = (Key) o;
                return Objects.equals(roleId, key.roleId) && Objects.equals(centerId, key.centerId);
            }
            @Override public int hashCode() { return Objects.hash(roleId, centerId); }
        }
        Set<Key> incoming = requests.stream()
                .map(r -> new Key(r.getRoleId(), r.getCenterId()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Load roles & centers in batch
        Map<Integer, Role> roleMap = roleRepository.findAllById(
                incoming.stream().map(k -> k.roleId).toList()
        ).stream().collect(Collectors.toMap(Role::getRoleId, r -> r));

        Map<Integer, Center> centerMap = centerRepository.findAllById(
                incoming.stream().map(k -> k.centerId).filter(Objects::nonNull).toList()
        ).stream().collect(Collectors.toMap(Center::getCenterId, c -> c));

        // Existing active keys for idempotency
        Set<Key> existingKeys = current.stream()
                .map(ur -> new Key(ur.getRole().getRoleId(), ur.getCenter() == null ? null : ur.getCenter().getCenterId()))
                .collect(Collectors.toSet());

        long currentCenterCount = current.stream()
                .filter(ur -> !RoleScopeUtil.isExclusiveGlobal(ur.getRole().getCode()))
                .count();

        List<UserRole> toCreate = new ArrayList<>();
        long newCenterCount = 0;

        for (Key k : incoming) {
            Role role = roleMap.get(k.roleId);
            if (role == null) throw new NotFoundException("Role not found: id=" + k.roleId);

            String code = role.getCode();
            boolean isGlobal = RoleScopeUtil.isExclusiveGlobal(code);
            boolean isCenterScoped = RoleScopeUtil.isCenterScoped(code) || !isGlobal;

            if (isGlobal && k.centerId != null) {
                throw new BadRequestException("Global role must be assigned without centerId");
            }
            if (isCenterScoped && k.centerId == null) {
                throw new BadRequestException("Center-scoped role requires centerId");
            }
            if (isGlobal && !current.isEmpty()) {
                throw new BadRequestException("Cannot assign a GLOBAL role to a user who already has active roles");
            }
            if (existingKeys.contains(k)) {
                continue; // idempotent skip
            }

            Center center = null;
            if (k.centerId != null) {
                center = centerMap.get(k.centerId);
                if (center == null) throw new NotFoundException("Center not found: id=" + k.centerId);
            }

            if (isCenterScoped) newCenterCount++;

            UserRole ur = new UserRole();
            ur.setUser(user);
            ur.setRole(role);
            ur.setCenter(center);
            ur.setAssignedBy(assignedBy);
            ur.setAssignedAt(LocalDateTime.now());
            ur.setCreatedAt(LocalDateTime.now());
            toCreate.add(ur);
        }

        // Enforce center-roles soft limit
        if (currentCenterCount + newCenterCount > MAX_ACTIVE_CENTER_ROLES_PER_USER) {
            throw new BadRequestException("Exceeds max center roles per user (" + MAX_ACTIVE_CENTER_ROLES_PER_USER + ")");
        }

        if (toCreate.isEmpty()) return List.of();

        List<UserRole> saved = userRoleRepository.saveAll(toCreate);
        return saved.stream().map(this::toDto).toList();
    }

    /** Soft revoke ONE (no-op if already revoked). */
    @Override
    public void revokeRoleFromUser(Integer userRoleId, String revokedBy) {
        if (userRoleId == null) throw new BadRequestException("UserRole ID must not be null");
        UserRole ur = userRoleRepository.findById(userRoleId)
                .orElseThrow(() -> new NotFoundException("UserRole not found: id=" + userRoleId));
        if (ur.getRevokedAt() != null) return;
        ur.setRevokedAt(LocalDateTime.now());
        ur.setRevokedBy(revokedBy);
        userRoleRepository.save(ur);
    }

    /** Soft revoke MANY (bulk & efficient). */
    @Override
    public void revokeRolesFromUsers(List<Integer> userRoleIds, String revokedBy) {
        if (userRoleIds == null || userRoleIds.isEmpty()) return;
        userRoleRepository.markRevokedByIds(userRoleIds, LocalDateTime.now(), revokedBy);
    }

    /** List active assignments in a center (paged). */
    @Override
    @Transactional(readOnly = true)
    public List<UserRoleResponse> getUserRolesByCenterId(Integer centerId, Integer page, Integer size) {
        if (centerId == null) throw new BadRequestException("Center ID must not be null");
        Pageable pageable = toPageable(page, size);
        Page<UserRole> pageResult = userRoleRepository.pageActiveByCenterId(centerId, pageable);
        return pageResult.getContent().stream().map(this::toDto).collect(Collectors.toList());
    }

    /** List active assignments of a user (all). */
    @Override
    @Transactional(readOnly = true)
    public List<UserRoleResponse> getUserRolesByUserId(Integer userId) {
        if (userId == null) throw new BadRequestException("User ID must not be null");
        List<UserRole> userRoles = userRoleRepository.findActiveByUserId(userId);
        return userRoles.stream().map(this::toDto).collect(Collectors.toList());
    }

    /** Check if user has role at center (centerId may be null for GLOBAL). */
    @Override
    @Transactional(readOnly = true)
    public boolean hasRoleAtCenter(Integer userId, String roleCode, Integer centerId) {
        if (userId == null) throw new BadRequestException("User ID must not be null");
        if (roleCode == null || roleCode.trim().isEmpty()) {
            throw new BadRequestException("Role code must not be blank");
        }
        return userRoleRepository.userHasActiveRoleByUserIdAndRoleCodeAndCenterId(userId, roleCode, centerId);
    }

    // ===== Helpers =====

    private Pageable toPageable(Integer page, Integer size) {
        int p = (page == null || page < 0) ? DEFAULT_PAGE : page;
        int s = (size == null || size <= 0) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(p, s);
    }

    private UserRoleResponse toDto(UserRole ur) {
        // user
        User u = ur.getUser();
        UserResponse userDto = new UserResponse();
        userDto.setUserId(u.getUserId());
        userDto.setFullName(u.getFullName());
        userDto.setEmail(u.getEmail());
        userDto.setPhone(u.getPhone());
        userDto.setKeycloakUserId(u.getKeycloakUserId());
        userDto.setDob(u.getDob());
        userDto.setGender(u.getGender() != null ? u.getGender().toString() : null);
        userDto.setActive(u.isActive());
        userDto.setCreatedAt(u.getCreatedAt());
        userDto.setUpdatedAt(u.getUpdatedAt());

        // role
        Role r = ur.getRole();
        RoleResponse roleDto = new RoleResponse();
        roleDto.setRoleId(r.getRoleId());
        roleDto.setCode(r.getCode());
        roleDto.setName(r.getName());
        roleDto.setActive(r.isActive());

        // center
        CenterResponse centerDto = null;
        if (ur.getCenter() != null) {
            Center c = ur.getCenter();
            centerDto = new CenterResponse();
            centerDto.setCenterId(c.getCenterId());
            centerDto.setName(c.getName());
            centerDto.setCode(c.getCode());
            centerDto.setEmail(c.getEmail());
            centerDto.setPhone(c.getPhone());
            centerDto.setEstablishedDate(c.getEstablishedDate());
            centerDto.setDescription(c.getDescription());
            centerDto.setAddressLine(c.getAddressLine());
            centerDto.setProvince(c.getProvince());
            centerDto.setDistrict(c.getDistrict());
            centerDto.setWard(c.getWard());
            centerDto.setCreatedBy(c.getCreatedBy());
            centerDto.setUpdatedBy(c.getUpdatedBy());
            centerDto.setCreatedAt(c.getCreatedAt());
            centerDto.setUpdatedAt(c.getUpdatedAt());
            centerDto.setDeletedAt(c.getDeletedAt());
        }

        UserRoleResponse dto = new UserRoleResponse();
        dto.setUserRoleId(ur.getUserRoleId());
        dto.setUser(userDto);
        dto.setRole(roleDto);
        dto.setCenter(centerDto);
        dto.setAssignedAt(ur.getAssignedAt());
        dto.setAssignedBy(ur.getAssignedBy());
        dto.setRevokedAt(ur.getRevokedAt());
        dto.setRevokedBy(ur.getRevokedBy());
        return dto;
    }
}

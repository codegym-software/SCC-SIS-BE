package com.example.sis.services.impl;

import com.example.sis.dtos.userrole.UserRoleRequest;
import com.example.sis.dtos.userrole.UserRoleResponse;
import com.example.sis.enums.RoleScope;
import com.example.sis.dtos.userrole.UserRoleAssignmentSummaryResponse;
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
        if (request.getUserId() == null) throw new BadRequestException("User ID không được để trống");
        if (request.getRoleId() == null) throw new BadRequestException("Role ID không được để trống");

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng với ID: " + request.getUserId()));
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy vai trò với ID: " + request.getRoleId()));

        String roleCode = role.getCode();
        boolean isGlobal = RoleScopeUtil.isExclusiveGlobal(roleCode);
        boolean isCenterScoped = RoleScopeUtil.isCenterScoped(roleCode) || !isGlobal;

        if (isGlobal && request.getCenterId() != null) {
            throw new BadRequestException("Vai trò toàn hệ thống (GLOBAL) không được gán trung tâm");
        }
        if (isCenterScoped && request.getCenterId() == null) {
            throw new BadRequestException("Thiếu trung tâm cho vai trò thuộc loại CENTER");
        }

        // Load current actives to enforce exclusivity / limits
        List<UserRole> current = userRoleRepository.findActiveByUserId(request.getUserId());
        boolean hasAnyGlobalActive = current.stream()
                .anyMatch(ur -> RoleScopeUtil.isExclusiveGlobal(ur.getRole().getCode()));
        if (hasAnyGlobalActive) {
            throw new BadRequestException("Người dùng này đã có vai trò toàn hệ thống (GLOBAL)");
        }
        if (isGlobal && !current.isEmpty()) {
            throw new BadRequestException("Không thể gán vai trò toàn hệ thống (GLOBAL) cho người dùng đã có vai trò khác");
        }

        long currentCenterCount = current.stream()
                .filter(ur -> !RoleScopeUtil.isExclusiveGlobal(ur.getRole().getCode()))
                .count();
        if (isCenterScoped && currentCenterCount + 1 > MAX_ACTIVE_CENTER_ROLES_PER_USER) {
            throw new BadRequestException("Người dùng này đã đạt giới hạn " + MAX_ACTIVE_CENTER_ROLES_PER_USER + " vai trò trung tâm");
        }

        // Idempotent: check if already has active assignment
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

        // Prevent re-assignment after revoke: check if user has ever had this role (including revoked)
        boolean hasEverHad = userRoleRepository.hasEverHadRole(
                request.getUserId(), request.getRoleId(), request.getCenterId());
        if (hasEverHad) {
            throw new BadRequestException("Không thể gán lại vai trò này vì đã từng bị hủy gán trước đó");
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
        if (userId == null) throw new BadRequestException("User ID không được để trống");
        if (requests == null || requests.isEmpty()) return List.of();

        // Normalize: set userId for all items; ensure same user
        for (UserRoleRequest r : requests) {
            if (r == null) throw new BadRequestException("Có item không hợp lệ trong danh sách");
            if (r.getRoleId() == null) throw new BadRequestException("Role ID không được để trống");
            if (r.getUserId() == null) r.setUserId(userId);
            if (!Objects.equals(r.getUserId(), userId)) {
                throw new BadRequestException("Tất cả items phải thuộc cùng một user");
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng với ID: " + userId));

        // Current actives & exclusivity pre-check
        List<UserRole> current = userRoleRepository.findActiveByUserId(userId);
        boolean hasAnyGlobalActive = current.stream()
                .anyMatch(ur -> RoleScopeUtil.isExclusiveGlobal(ur.getRole().getCode()));
        if (hasAnyGlobalActive) {
            throw new BadRequestException("Người dùng này đã có vai trò toàn hệ thống (GLOBAL)");
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
            if (role == null) throw new NotFoundException("Không tìm thấy vai trò với ID: " + k.roleId);

            String code = role.getCode();
            boolean isGlobal = RoleScopeUtil.isExclusiveGlobal(code);
            boolean isCenterScoped = RoleScopeUtil.isCenterScoped(code) || !isGlobal;

            if (isGlobal && k.centerId != null) {
                throw new BadRequestException("Vai trò toàn hệ thống (GLOBAL) không được gán trung tâm");
            }
            if (isCenterScoped && k.centerId == null) {
                throw new BadRequestException("Thiếu trung tâm cho vai trò thuộc loại CENTER");
            }
            if (isGlobal && !current.isEmpty()) {
                throw new BadRequestException("Không thể gán vai trò toàn hệ thống (GLOBAL) cho người dùng đã có vai trò khác");
            }
            if (existingKeys.contains(k)) {
                continue; // idempotent skip
            }

            Center center = null;
            if (k.centerId != null) {
                center = centerMap.get(k.centerId);
                if (center == null) throw new NotFoundException("Không tìm thấy trung tâm với ID: " + k.centerId);
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
            throw new BadRequestException("Người dùng này đã đạt giới hạn " + MAX_ACTIVE_CENTER_ROLES_PER_USER + " vai trò trung tâm");
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

    /** List revoked roles of a user (to check which roles cannot be re-assigned). */
    @Override
    @Transactional(readOnly = true)
    public List<UserRoleResponse> getRevokedRolesByUserId(Integer userId) {
        if (userId == null) throw new BadRequestException("User ID must not be null");
        List<UserRole> revokedRoles = userRoleRepository.findRevokedByUserId(userId);
        return revokedRoles.stream().map(this::toDto).collect(Collectors.toList());
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
    @Override
    @Transactional
    public void assignIfNotExists(Long userId, Integer roleId, RoleScope scope, Integer centerId) {
        // Guard
        if (userId == null || roleId == null || scope == null) return;

        // GLOBAL rules
        if (scope == RoleScope.GLOBAL) {
            // GLOBAL không đi kèm center
            centerId = null;
            // Nếu đã có GLOBAL → dừng
            if (userRoleRepository.existsGlobalByUserId(userId)) return;
        }

        // CENTER rules
        if (scope == RoleScope.CENTER) {
            // CENTER phải có centerId
            if (centerId == null) return;
            // Tối đa 3 vai trò CENTER
            long cnt = userRoleRepository.countCenterAssignments(userId);
            if (cnt >= MAX_ACTIVE_CENTER_ROLES_PER_USER) return;
        }

        // Idempotent: đã có (user, role, center) → bỏ qua
        if (userRoleRepository.existsByUserIdAndRoleIdAndCenterId(userId, roleId, centerId)) return;

        // --- Nạp entity để lưu (User/Role dùng Integer id) ---
        Integer userIdInt;
        try {
            userIdInt = Math.toIntExact(userId); // User.userId là Integer
        } catch (ArithmeticException ex) {
            // Không thể chuyển kiểu an toàn → bỏ qua để không chặn request auto-assign
            return;
        }

        var userOpt = userRepository.findById(userIdInt);
        if (userOpt.isEmpty()) return;
        var roleOpt = roleRepository.findById(roleId);
        if (roleOpt.isEmpty()) return;

        var user = userOpt.get();
        var role = roleOpt.get();

        var center = (centerId != null)
                ? centerRepository.findById(centerId).orElse(null)
                : null;
        if (scope == RoleScope.GLOBAL && center != null) center = null; // đảm bảo GLOBAL không có center

        // Lưu
        var ur = new UserRole();
        ur.setUser(user);
        ur.setRole(role);
        ur.setCenter(center);
        ur.setAssignedBy("SYSTEM_AUTO");
        ur.setAssignedAt(LocalDateTime.now());
        ur.setCreatedAt(LocalDateTime.now());
        userRoleRepository.save(ur);
    }
    /**
     * Assign MANY roles to user with summary response (Vietnamese error messages).
     * - Validates business rules with Vietnamese messages.
     * - Enforces: GLOBAL ≤ 1, CENTER ≤ 3.
     * - Idempotent: skips existing assignments.
     * - Returns summary with counts and errors.
     * - Checks rules AFTER "removing" old roles.
     */
    @Override
    public UserRoleAssignmentSummaryResponse assignRolesToUserWithSummary(Integer userId, List<UserRoleRequest> requests, String assignedBy) {
        UserRoleAssignmentSummaryResponse summary = new UserRoleAssignmentSummaryResponse();

        if (userId == null) {
            summary.addError("User ID không được để trống");
            return summary;
        }
        if (requests == null || requests.isEmpty()) {
            return summary;
        }

        // Normalize: set userId for all items; ensure same user
        for (UserRoleRequest r : requests) {
            if (r == null) {
                summary.addError("Có item không hợp lệ trong danh sách");
                continue;
            }
            if (r.getRoleId() == null) {
                summary.addError("Role ID không được để trống");
                continue;
            }
            if (r.getUserId() == null) r.setUserId(userId);
            if (!Objects.equals(r.getUserId(), userId)) {
                summary.addError("Tất cả items phải thuộc cùng một user");
                return summary;
            }
        }

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng với ID: " + userId));

            // Lấy danh sách ACTIVE hiện tại
            List<UserRole> currentActiveRoles = userRoleRepository.findActiveByUserId(userId);

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

            // Lọc các request hợp lệ và loại bỏ trùng lặp
            Set<Key> incomingKeys = new LinkedHashSet<>();
            List<UserRoleRequest> validRequests = new ArrayList<>();

            for (UserRoleRequest r : requests) {
                if (r == null || r.getRoleId() == null) continue;

                Key key = new Key(r.getRoleId(), r.getCenterId());
                if (incomingKeys.contains(key)) {
                    summary.incrementSkipped(); // Trùng lặp trong request
                    continue;
                }
                incomingKeys.add(key);
                validRequests.add(r);
            }

            if (validRequests.isEmpty()) {
                return summary;
            }

            // Load roles & centers in batch
            Map<Integer, Role> roleMap = roleRepository.findAllById(
                    validRequests.stream().map(UserRoleRequest::getRoleId).toList()
            ).stream().collect(Collectors.toMap(Role::getRoleId, r -> r));

            Map<Integer, Center> centerMap = centerRepository.findAllById(
                    validRequests.stream().map(r -> r.getCenterId()).filter(Objects::nonNull).toList()
            ).stream().collect(Collectors.toMap(Center::getCenterId, c -> c));

            // Kiểm tra từng request và xử lý idempotent
            List<UserRole> toCreate = new ArrayList<>();
            Set<Key> existingKeys = currentActiveRoles.stream()
                    .map(ur -> new Key(ur.getRole().getRoleId(), ur.getCenter() == null ? null : ur.getCenter().getCenterId()))
                    .collect(Collectors.toSet());

            for (UserRoleRequest r : validRequests) {
                Role role = roleMap.get(r.getRoleId());
                if (role == null) {
                    summary.addError("Không tìm thấy role với ID: " + r.getRoleId());
                    continue;
                }

                String code = role.getCode();
                boolean isGlobal = RoleScopeUtil.isExclusiveGlobal(code);
                boolean isCenterScoped = RoleScopeUtil.isCenterScoped(code) || !isGlobal;

                // Kiểm tra validation cơ bản
                if (isGlobal && r.getCenterId() != null) {
                    summary.addError("Vai trò toàn hệ thống (GLOBAL) không được gán trung tâm");
                    continue;
                }
                if (isCenterScoped && r.getCenterId() == null) {
                    summary.addError("Thiếu trung tâm cho vai trò thuộc loại CENTER");
                    continue;
                }

                // Kiểm tra idempotent - nếu đã tồn tại thì skip
                Key key = new Key(r.getRoleId(), r.getCenterId());
                if (existingKeys.contains(key)) {
                    summary.incrementSkipped();
                    continue;
                }

                // Kiểm tra center tồn tại trước
                Center center = null;
                if (r.getCenterId() != null) {
                    center = centerMap.get(r.getCenterId());
                    if (center == null) {
                        summary.addError("Không tìm thấy trung tâm với ID: " + r.getCenterId());
                        continue;
                    }
                }

                // Prevent re-assignment after revoke: check if user has ever had this role (including revoked)
                boolean hasEverHad = userRoleRepository.hasEverHadRole(
                        userId, r.getRoleId(), r.getCenterId());
                if (hasEverHad) {
                    String errorMsg = "Không thể gán lại vai trò \"" + role.getName() + "\"";
                    if (center != null) {
                        errorMsg += " tại trung tâm \"" + center.getName() + "\"";
                    }
                    errorMsg += " vì đã từng bị hủy gán trước đó";
                    summary.addError(errorMsg);
                    continue;
                }

                // Tạo UserRole để thêm vào danh sách sẽ xử lý
                UserRole ur = new UserRole();
                ur.setUser(user);
                ur.setRole(role);
                ur.setCenter(center);
                ur.setAssignedBy(assignedBy);
                ur.setAssignedAt(LocalDateTime.now());
                ur.setCreatedAt(LocalDateTime.now());
                toCreate.add(ur);
            }

            if (toCreate.isEmpty()) {
                return summary;
            }

            // Tính toán danh sách roles sẽ có sau khi assign (bao gồm current + new)
            List<UserRole> finalRoles = new ArrayList<>(currentActiveRoles);
            finalRoles.addAll(toCreate);

            // Kiểm tra rule SAU KHI "xóa" các vai trò cũ
            long globalCount = finalRoles.stream()
                    .filter(ur -> RoleScopeUtil.isExclusiveGlobal(ur.getRole().getCode()))
                    .count();

            long centerCount = finalRoles.stream()
                    .filter(ur -> !RoleScopeUtil.isExclusiveGlobal(ur.getRole().getCode()))
                    .count();

            // Kiểm tra ràng buộc GLOBAL ≤ 1
            if (globalCount > 1) {
                summary.addError("Người dùng này chỉ được có tối đa 1 vai trò toàn hệ thống (GLOBAL)");
                return summary;
            }

            // Kiểm tra ràng buộc CENTER ≤ 3
            if (centerCount > MAX_ACTIVE_CENTER_ROLES_PER_USER) {
                summary.addError("Người dùng này chỉ được có tối đa " + MAX_ACTIVE_CENTER_ROLES_PER_USER + " vai trò trung tâm");
                return summary;
            }

            // Nếu tất cả kiểm tra đều pass, thực hiện insert
            List<UserRole> saved = userRoleRepository.saveAll(toCreate);
            summary.setCreatedCount(saved.size());

        } catch (Exception e) {
            summary.addError("Lỗi hệ thống: " + e.getMessage());
        }

        return summary;
    }
    
}

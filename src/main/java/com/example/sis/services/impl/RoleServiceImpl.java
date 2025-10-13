package com.example.sis.services.impl;

import com.example.sis.enums.RoleScope; // <— đường dẫn đúng
import com.example.sis.dtos.role.CreateRoleRequest;
import com.example.sis.dtos.role.RoleResponse;
import com.example.sis.dtos.role.RoleListResponse;
import com.example.sis.dtos.role.RoleListItem;
import com.example.sis.dtos.role.UpdateRoleRequest;
import com.example.sis.models.Role;
import com.example.sis.models.Permission;
import com.example.sis.models.RolePermission;
import com.example.sis.repositories.RoleRepository;
import com.example.sis.repositories.PermissionRepository;
import com.example.sis.repositories.RolePermissionRepository;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.securities.AuthzService;
import com.example.sis.services.RoleService;
import com.example.sis.utils.RoleScopeUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuthzService authzService;

    public RoleServiceImpl(RoleRepository roleRepository,
             PermissionRepository permissionRepository,
             RolePermissionRepository rolePermissionRepository,
             UserRoleRepository userRoleRepository,
             AuthzService authzService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRoleRepository = userRoleRepository;
        this.authzService = authzService;
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
        // Validation: kiểm tra mã role đã tồn tại
        if (roleRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Mã role đã tồn tại: " + request.getCode());
        }

        // Note: Không validate tên trùng vì có thể có nhiều role cùng tên khác scope

        // Tạo role mới
        Role role = new Role();
        role.setCode(request.getCode());
        role.setName(request.getName());
        role.setActive(request.getActive() != null ? request.getActive() : true);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());

        Role saved = roleRepository.save(role);

        Set<Integer> assignedPermissionIds = new HashSet<>();
        Map<String, Object> summary = new HashMap<>();

        // Gán quyền cho role nếu có permissionIds trong request
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            // Validate tất cả permissions tồn tại
            List<Permission> permissions = permissionRepository.findAllById(request.getPermissionIds());
            if (permissions.size() != request.getPermissionIds().size()) {
                throw new RuntimeException("Một hoặc nhiều permission ID không tồn tại");
            }

            // Lấy current user ID từ authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserId = authzService.getCurrentUserId(authentication);

            // Tạo role_permissions records với currentUserId
            List<RolePermission> rolePermissions = permissions.stream()
                .map(permission -> new RolePermission(saved, permission, currentUserId))
                .collect(Collectors.toList());

            rolePermissionRepository.saveAll(rolePermissions);

            // Lưu permission IDs đã gán thành công
            assignedPermissionIds = request.getPermissionIds();

            // Build summary
            summary = buildPermissionSummary(permissions);
        }

        return new RoleResponse(
            saved.getRoleId(),
            saved.getCode(),
            saved.getName(),
            saved.isActive(),
            saved.getCreatedAt(),
            saved.getUpdatedAt(),
            0L, // userCount sẽ được tính sau nếu cần
            assignedPermissionIds,
            summary
        );
    }

    @Override
    @Transactional
    public RoleResponse updateRole(Integer roleId, UpdateRoleRequest request) {
        // 1. Load role theo ID
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại với ID: " + roleId));

        // 2. Load thông tin hiện tại để so sánh
        String currentCode = role.getCode();
        String currentName = role.getName();
        Boolean currentActive = role.isActive();
        List<Permission> currentPermissions = permissionRepository.findByRoleId(roleId);
        Set<Integer> currentPermissionIds = currentPermissions.stream()
            .map(Permission::getPermissionId)
            .collect(Collectors.toSet());

        // 3. Kiểm tra xem có gì thay đổi không
        boolean hasCodeChanged = request.getCode() != null && !request.getCode().equals(currentCode);
        boolean hasNameChanged = request.getName() != null && !request.getName().equals(currentName);
        boolean hasActiveChanged = request.getActive() != null && !request.getActive().equals(currentActive);
        boolean hasPermissionsChanged = request.getPermissionIds() != null &&
            (currentPermissionIds.size() != request.getPermissionIds().size() ||
             !currentPermissionIds.containsAll(request.getPermissionIds()));

        // Nếu không có gì thay đổi, trả về thông tin hiện tại
        if (!hasCodeChanged && !hasNameChanged && !hasActiveChanged && !hasPermissionsChanged) {
            Map<String, Object> summary = buildPermissionSummary(currentPermissions);
            return new RoleResponse(
                role.getRoleId(),
                role.getCode(),
                role.getName(),
                role.isActive(),
                role.getCreatedAt(),
                role.getUpdatedAt(),
                userRoleRepository.countByRoleId(roleId),
                currentPermissionIds,
                summary
            );
        }

        // 4. Validate unique constraints cho các field được thay đổi
        if (hasCodeChanged) {
            if (roleRepository.existsByCode(request.getCode())) {
                throw new RuntimeException("Mã role đã tồn tại: " + request.getCode());
            }
        }

        // 5. Cập nhật thông tin cơ bản
        if (hasCodeChanged) {
            role.setCode(request.getCode());
        }
        if (hasNameChanged) {
            role.setName(request.getName());
        }
        if (hasActiveChanged) {
            role.setActive(request.getActive());
        }
        role.setUpdatedAt(LocalDateTime.now());

        // 6. Xử lý permissions nếu có thay đổi
        Set<Integer> assignedPermissionIds = currentPermissionIds;
        List<Permission> finalPermissions = currentPermissions;

        if (hasPermissionsChanged && request.getPermissionIds() != null) {
            // Xử lý permissions một cách thông minh hơn
            if (!request.getPermissionIds().isEmpty()) {
                // Lấy danh sách permissions mới
                List<Permission> permissions = permissionRepository.findAllById(request.getPermissionIds());
                if (permissions.size() != request.getPermissionIds().size()) {
                    throw new RuntimeException("Một hoặc nhiều permission ID không tồn tại");
                }

                // Tìm permissions cần thêm (có trong request nhưng không có trong current)
                Set<Integer> permissionsToAdd = new HashSet<>(request.getPermissionIds());
                permissionsToAdd.removeAll(currentPermissionIds);

                // Tìm permissions cần xóa (có trong current nhưng không có trong request)
                Set<Integer> permissionsToRemove = new HashSet<>(currentPermissionIds);
                permissionsToRemove.removeAll(request.getPermissionIds());

                // Lấy current user ID từ authentication
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String currentUserId = authzService.getCurrentUserId(authentication);

                // Xóa permissions không còn cần thiết
                if (!permissionsToRemove.isEmpty()) {
                    rolePermissionRepository.deleteByRoleIdAndPermissionIds(role.getRoleId(), new ArrayList<>(permissionsToRemove));
                }

                // Thêm permissions mới
                if (!permissionsToAdd.isEmpty()) {
                    List<Permission> newPermissions = permissionRepository.findAllById(permissionsToAdd);
                    List<RolePermission> rolePermissionsToAdd = newPermissions.stream()
                        .map(permission -> new RolePermission(role, permission, currentUserId))
                        .collect(Collectors.toList());
                    rolePermissionRepository.saveAll(rolePermissionsToAdd);
                }

                // Lưu permission IDs đã gán thành công
                assignedPermissionIds = Set.copyOf(request.getPermissionIds());
                finalPermissions = permissions;
            } else {
                // PermissionIds rỗng, xoá toàn bộ quyền
                rolePermissionRepository.deleteByRole(role);
                assignedPermissionIds = Set.of();
                finalPermissions = List.of();
            }
        }

        // 7. Build summary từ permissions hiện tại
        Map<String, Object> summary = buildPermissionSummary(finalPermissions);

        // 8. Save role
        Role saved = roleRepository.save(role);

        // 9. Trả về response với đầy đủ thông tin
        return new RoleResponse(
            saved.getRoleId(),
            saved.getCode(),
            saved.getName(),
            saved.isActive(),
            saved.getCreatedAt(),
            saved.getUpdatedAt(),
            userRoleRepository.countByRoleId(roleId),
            assignedPermissionIds,
            summary
        );
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

        // Load permissions của role
        List<Permission> rolePerms = permissionRepository.findByRoleId(roleId);
        Set<Integer> permissionIds = rolePerms.stream()
                .map(Permission::getPermissionId)
                .collect(Collectors.toSet());

        // Build summary
        Map<String, Object> summary = buildPermissionSummary(rolePerms);

        // Count users có role này
        long userCount = userRoleRepository.countByRoleId(roleId);

        return new RoleResponse(
                role.getRoleId(),
                role.getCode(),
                role.getName(),
                role.isActive(),
                role.getCreatedAt(),
                role.getUpdatedAt(),
                userCount,
                permissionIds,
                summary
        );
    }

    // === Helpers ===
    private RoleResponse convertToResponse(Role role) {
        return new RoleResponse(
                role.getRoleId(),
                role.getCode(),
                role.getName(),
                role.isActive(),
                role.getCreatedAt(),
                role.getUpdatedAt(),
                0L, // userCount sẽ được tính riêng nếu cần
                Set.of(), // permissionIds sẽ được load riêng nếu cần
                Map.of() // summary sẽ được build riêng nếu cần
        );
    }

    private Map<String, Object> buildPermissionSummary(List<Permission> permissions) {
        // 1. Lấy tổng số quyền active theo category từ DB
        List<Object[]> totalResults = permissionRepository.countActiveGroupByCategory();
        Map<String, Long> totalByCategory = totalResults.stream()
            .collect(Collectors.toMap(
                result -> (String) result[0],
                result -> (Long) result[1]
            ));

        // 2. Gom nhóm permissions của role theo category và extract actions
        Map<String, List<String>> actionsByCategory = new HashMap<>();
        for (Permission p : permissions) {
            if (Boolean.TRUE.equals(p.getActive())) {
                String cat = p.getCategory() != null ? p.getCategory() : "MISC";
                String action = extractActionFromCode(p.getCode());

                // Loại bỏ các quyền *_MANAGE
                if (!action.endsWith("_MANAGE") && !action.equals("MANAGE")) {
                    actionsByCategory.computeIfAbsent(cat, k -> new ArrayList<>()).add(action);
                }
            }
        }

        // 3. Thứ tự ưu tiên cho CRUD actions
        List<String> crudOrder = List.of("READ", "CREATE", "UPDATE", "DELETE");

        // 4. Xây summary với total từ DB
        Map<String, Object> summary = new LinkedHashMap<>();
        for (var entry : totalByCategory.entrySet()) {
            String category = entry.getKey();
            long total = entry.getValue();
            List<String> actions = actionsByCategory.getOrDefault(category, List.of());

            // Remove duplicates và sort theo CRUD order trước
            List<String> uniqueActions = actions.stream()
                .distinct()
                .collect(Collectors.toList());

            // Sort: CRUD actions trước, sau đó alphabetical
            uniqueActions.sort((a, b) -> {
                int indexA = crudOrder.indexOf(a);
                int indexB = crudOrder.indexOf(b);

                if (indexA != -1 && indexB != -1) {
                    return Integer.compare(indexA, indexB);
                }
                if (indexA != -1) return -1;
                if (indexB != -1) return 1;
                return a.compareTo(b);
            });

            summary.put(category, Map.of(
                "total", total,
                "count", uniqueActions.size(),
                "actions", uniqueActions
            ));
        }

        return summary;
    }

    private String extractActionFromCode(String code) {
        if (code == null) return "";

        // Find the last '_' or ':' and get the part after it
        int lastUnderscore = code.lastIndexOf('_');
        int lastColon = code.lastIndexOf(':');

        int lastIndex = Math.max(lastUnderscore, lastColon);
        if (lastIndex >= 0 && lastIndex < code.length() - 1) {
            return code.substring(lastIndex + 1);
        }

        return code;
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

    @Override
    @Transactional(readOnly = true)
    public RoleListResponse listRolesNew(Integer previewLimit) {
        // Set default preview limit to 4 if null
        int limit = previewLimit != null ? previewLimit : 4;

        // 1. Lấy tất cả roles active sắp xếp theo createdAt
        List<Role> roles = roleRepository.findByActiveTrueOrderByCreatedAtAsc();
        List<Integer> roleIds = roles.stream()
                .map(Role::getRoleId)
                .toList();

        if (roleIds.isEmpty()) {
            return new RoleListResponse(0, List.of());
        }

        // 2. Đếm user cho từng role (batch query)
        Map<Integer, Long> userCountMap = new HashMap<>();
        List<Object[]> userCountResults = userRoleRepository.countByRoleIdInGroup(roleIds);
        for (Object[] result : userCountResults) {
            Integer roleId = (Integer) result[0];
            Long count = (Long) result[1];
            userCountMap.put(roleId, count);
        }

        // 3. Lấy toàn bộ quyền của các role (batch query với JOIN)
        Map<Integer, List<String>> permissionNamesMap = new HashMap<>();
        Map<Integer, Integer> permissionCountMap = new HashMap<>();

        List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleIdInWithPermission(roleIds);
        for (RolePermission rp : rolePermissions) {
            Integer roleId = rp.getRole().getRoleId();
            String permissionName = rp.getPermission().getName();

            // Build permission names map (sorted by category, name)
            permissionNamesMap.computeIfAbsent(roleId, k -> new ArrayList<>()).add(permissionName);

            // Count permissions
            permissionCountMap.merge(roleId, 1, Integer::sum);
        }

        // Sort permission names by category and name for consistent preview
        for (Map.Entry<Integer, List<String>> entry : permissionNamesMap.entrySet()) {
            Integer roleId = entry.getKey();
            List<String> names = entry.getValue();
            // Note: In real implementation, you'd sort by category first, then name
            // For now, just sort by name for simplicity
            names.sort(String::compareTo);
        }

        // 4. Build DTO list
        List<RoleListItem> items = new ArrayList<>();
        for (Role role : roles) {
            Integer roleId = role.getRoleId();
            List<String> permissionNames = permissionNamesMap.getOrDefault(roleId, List.of());
            List<String> permissionPreview = permissionNames.stream()
                    .limit(limit)
                    .toList();

            RoleListItem item = new RoleListItem(
                role.getRoleId(),
                role.getCode(),
                role.getName(),
                role.isActive(),
                role.getCreatedAt(),
                userCountMap.getOrDefault(roleId, 0L),
                permissionCountMap.getOrDefault(roleId, 0),
                permissionPreview
            );
            items.add(item);
        }

        return new RoleListResponse(items.size(), items);
    }
}

package com.example.sis.services.impl;

import com.example.sis.dtos.permission.PermissionResponse;
import com.example.sis.dtos.permission.PermissionGroupResponse;
import com.example.sis.dtos.permission.PermissionGroupItemResponse;
import com.example.sis.enums.PermissionCategory;
import com.example.sis.exceptions.NotFoundException;
import com.example.sis.models.Permission;
import com.example.sis.repositories.PermissionRepository;
import com.example.sis.services.PermissionService;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Permissions management implementation (Super Admin only).
 *
 * CHÍNH: groups() - Method duy nhất cho mọi nhu cầu UI
 * - Thay thế toàn bộ các method cũ: search, listCategories, getById
 * - Hỗ trợ tìm kiếm, lọc, phân nhóm, đếm tổng, trạng thái granted
 */
@Service
@Transactional
public class PermissionServiceImpl implements PermissionService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 50;
    private static final int MAX_SIZE = 200;
    private static final String DEFAULT_SORT = "name,asc";

    private final PermissionRepository permissionRepository;

    public PermissionServiceImpl(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    // ===== DEPRECATED: Unified listing (replaced by groups()) =====
    @Override
    @Deprecated
    @Transactional(readOnly = true)
    public List<PermissionResponse> search(String q, String category, Boolean active,
                                            Integer page, Integer size, String sort) {

        String qNorm = normalize(q);
        String catNorm = normalize(category);
        Boolean act = (active == null) ? Boolean.TRUE : active; // default: only active

        Pageable pageable = toPageable(page, size, sort);
        Page<Permission> result = permissionRepository.search(qNorm, catNorm, act, pageable);

        return result.getContent().stream().map(this::toDto).toList();
    }

    // ===== Groups for UI =====
    @Override
    @Transactional(readOnly = true)
    public List<PermissionGroupResponse> groups(String q, String category, Boolean activeOnly,
                                                Integer roleId, Boolean includeEmpty) {

        String qNorm = normalize(q);
        String catNorm = normalize(category);
        Boolean actOnly = (activeOnly == null) ? Boolean.TRUE : activeOnly; // default: only active
        Boolean incEmpty = (includeEmpty == null) ? Boolean.FALSE : includeEmpty; // default: exclude empty

        // Get all permissions with filters
        List<Permission> allPermissions = permissionRepository.search(qNorm, catNorm, actOnly, Pageable.unpaged()).getContent();

        // Get granted permission IDs if roleId provided
        final Set<Integer> grantedPermissionIds;
        if (roleId != null) {
            List<Permission> grantedPermissions = permissionRepository.findByRoleId(roleId);
            grantedPermissionIds = grantedPermissions.stream()
                    .map(Permission::getPermissionId)
                    .collect(Collectors.toSet());
        } else {
            grantedPermissionIds = new HashSet<>();
        }

        // Group by category and build response
        Map<String, List<Permission>> groupedByCategory = allPermissions.stream()
                .collect(Collectors.groupingBy(Permission::getCategory));

        List<PermissionGroupResponse> groups = new ArrayList<>();

        for (Map.Entry<String, List<Permission>> entry : groupedByCategory.entrySet()) {
            String categoryCode = entry.getKey();
            List<Permission> permissions = entry.getValue();

            // Skip empty groups if includeEmpty is false
            if (!incEmpty && permissions.isEmpty()) {
                continue;
            }

            // Get category info from enum
            PermissionCategory permissionCategory = PermissionCategory.fromCategory(categoryCode);

            // Build items
            List<PermissionGroupItemResponse> items = permissions.stream()
                    .sorted(Comparator.comparing(Permission::getName)) // Sort by name ASC
                    .map(p -> {
                        PermissionGroupItemResponse item = new PermissionGroupItemResponse();
                        item.setPermissionId(p.getPermissionId());
                        item.setCode(p.getCode());
                        item.setName(p.getName());
                        item.setActive(p.getActive());
                        // Set granted status if roleId provided
                        if (roleId != null) {
                            item.setGranted(grantedPermissionIds.contains(p.getPermissionId()));
                        }
                        return item;
                    })
                    .collect(Collectors.toList());

            // Build group response
            PermissionGroupResponse group = new PermissionGroupResponse();
            group.setCategory(categoryCode);
            group.setCategoryLabel(permissionCategory != null ? permissionCategory.getLabel() : categoryCode);
            group.setOrder(permissionCategory != null ? permissionCategory.getOrder() : 999);
            group.setTotal(items.size());
            group.setItems(items);

            groups.add(group);
        }

        // Sort groups by order
        groups.sort(Comparator.comparing(PermissionGroupResponse::getOrder));

        return groups;
    }

    // ===== DEPRECATED: Categories (replaced by groups()) =====
    @Override
    @Deprecated
    @Transactional(readOnly = true)
    public List<String> listCategories() {
        return permissionRepository.findDistinctCategoriesByActiveTrue();
    }

    // ===== DEPRECATED: Get by ID (replaced by groups()) =====
    @Override
    @Deprecated
    @Transactional(readOnly = true)
    public PermissionResponse getById(Integer id) {
        Permission p = permissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Permission not found: id=" + id));
        return toDto(p);
    }

    // ===== Legacy wrappers (kept to avoid breaking callers) =====
    @Override
    @Deprecated
    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions(Boolean active) {
        Boolean act = (active == null) ? Boolean.TRUE : active;
        // Use unified search with sensible defaults
        return search(null, null, act, DEFAULT_PAGE, DEFAULT_SIZE, DEFAULT_SORT);
    }

    @Override
    @Deprecated
    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissionsByCategory(String category) {
        return search(null, category, Boolean.TRUE, DEFAULT_PAGE, DEFAULT_SIZE, DEFAULT_SORT);
    }

    // ===== Helpers =====
    private PermissionResponse toDto(Permission permission) {
        return new PermissionResponse(
                permission.getPermissionId(),
                permission.getCode(),
                permission.getName(),
                permission.getDescription(),
                permission.getCategory(),
                permission.getActive(),
                permission.getCreatedAt(),
                permission.getUpdatedAt()
        );
    }

    private String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private Pageable toPageable(Integer page, Integer size, String sort) {
        int p = (page == null || page < 0) ? DEFAULT_PAGE : page;
        int s = (size == null || size <= 0) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        Sort sortSpec = parseSort(sort);
        return PageRequest.of(p, s, sortSpec);
    }

    private Sort parseSort(String sort) {
        String spec = (sort == null || sort.isBlank()) ? DEFAULT_SORT : sort.trim();
        // expected: "field,dir"
        String[] parts = spec.split(",", 2);
        String field = parts[0].trim();
        String dir = (parts.length > 1 ? parts[1].trim() : "asc").toLowerCase();

        Sort.Direction direction = "desc".equals(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        // Whitelist known sortable fields to avoid invalid property exceptions (optional hardening)
        // if (!List.of("name","code","category","createdAt","updatedAt").contains(field)) field = "name";
        return Sort.by(direction, field);
    }
}

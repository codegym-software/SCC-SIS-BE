package com.example.sis.services.impl;

import com.example.sis.dtos.permission.PermissionResponse;
import com.example.sis.exceptions.NotFoundException;
import com.example.sis.models.Permission;
import com.example.sis.repositories.PermissionRepository;
import com.example.sis.services.PermissionService;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    // ===== Unified listing =====
    @Override
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

    // ===== Categories =====
    @Override
    @Transactional(readOnly = true)
    public List<String> listCategories() {
        return permissionRepository.findDistinctCategoriesByActiveTrue();
    }

    // ===== Get by ID =====
    @Override
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

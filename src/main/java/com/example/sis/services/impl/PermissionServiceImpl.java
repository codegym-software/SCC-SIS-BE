package com.example.sis.services.impl;

import com.example.sis.dtos.permission.PermissionResponse;
import com.example.sis.exceptions.NotFoundException;
import com.example.sis.models.Permission;
import com.example.sis.repositories.PermissionRepository;
import com.example.sis.services.PermissionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    public PermissionServiceImpl(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    public List<PermissionResponse> listPermissions(Boolean active) {
        final List<Permission> permissions = (active == null || Boolean.TRUE.equals(active))
                ? permissionRepository.findByActiveTrueOrderByCategoryAscNameAsc()
                : permissionRepository.findAllByOrderByCategoryAscNameAsc();

        return permissions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionResponse> listPermissionsByCategory(String category) {
        final List<Permission> permissions = permissionRepository
                .findByCategoryAndActiveTrueOrderByNameAsc(category);

        return permissions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listCategories() {
        return permissionRepository.findDistinctCategoriesByActiveTrue();
    }

    @Override
    public PermissionResponse getPermissionById(Integer id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy permission với ID: " + id));

        return convertToResponse(permission);
    }

    /**
     * Convert Permission entity to PermissionResponse DTO
     */
    private PermissionResponse convertToResponse(Permission permission) {
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
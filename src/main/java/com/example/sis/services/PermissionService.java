package com.example.sis.services;

import com.example.sis.dtos.permission.PermissionResponse;
import java.util.List;

/**
 * Permissions management (Super Admin only).
 * Unified search for listing; categories & get-by-id supported.
 */
public interface PermissionService {

    /**
     * Unified listing with filters & pagination.
     * - active: null/true -> only active (default), false -> all
     * - q: fuzzy on code/name
     * - category: exact match
     * - page/size/sort: pagination (e.g. "name,asc")
     */
    List<PermissionResponse> search(String q, String category, Boolean active,
                                    Integer page, Integer size, String sort);

    /** Distinct categories (active only). */
    List<String> listCategories();

    /** Get a permission by ID. */
    PermissionResponse getById(Integer id);

    // ---- Legacy (kept for backward compatibility) ----
    @Deprecated
    List<PermissionResponse> listPermissions(Boolean active);

    @Deprecated
    List<PermissionResponse> listPermissionsByCategory(String category);
}

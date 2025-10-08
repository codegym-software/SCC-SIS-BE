package com.example.sis.controllers;


import com.example.sis.dtos.center.CenterLiteResponse;
import com.example.sis.dtos.center.CenterResponse;
import com.example.sis.dtos.center.CreateCenterRequest;
import com.example.sis.dtos.center.UpdateCenterRequest;
import com.example.sis.services.CenterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/centers")
public class CenterController {

    private final CenterService centerService;

    public CenterController(CenterService centerService) {
        this.centerService = centerService;
    }

    // ===== LITE cho dropdown FE =====
    @GetMapping("/lite")
    public ResponseEntity<List<CenterLiteResponse>> listCentersLite() {
        return ResponseEntity.ok(centerService.listCentersLite());
    }

    // ===== Quản trị (dev1 giữ nguyên) =====
    // Active-full
    @GetMapping
    @PreAuthorize("@authz.isSuperAdmin(authentication)")
    public ResponseEntity<List<CenterResponse>> getAllActiveCenters() {
        return ResponseEntity.ok(centerService.getAllActiveCenters());
    }

    // Toàn bộ (kể cả deactivated)
    @GetMapping("/all")
    @PreAuthorize("@authz.isSuperAdmin(authentication)")
    public ResponseEntity<List<CenterResponse>> getAllCenters() {
        return ResponseEntity.ok(centerService.getAllCenters());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.isSuperAdmin(authentication)")

    public ResponseEntity<CenterResponse> getCenterById(@PathVariable Integer id) {
        return ResponseEntity.ok(centerService.getCenterById(id));
    }

    @PostMapping
    @PreAuthorize("@authz.isSuperAdmin(authentication)")
    public ResponseEntity<CenterResponse> createCenter(
            @Valid @RequestBody CreateCenterRequest request,
            Authentication authentication) {
        Integer createdBy = getCurrentUserId(authentication);
        CenterResponse center = centerService.createCenter(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(center);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.isSuperAdmin(authentication)")
    public ResponseEntity<CenterResponse> updateCenter(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateCenterRequest request,
            Authentication authentication) {
        Integer updatedBy = getCurrentUserId(authentication);
        CenterResponse center = centerService.updateCenter(id, request, updatedBy);
        return ResponseEntity.ok(center);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.isSuperAdmin(authentication)")
    public ResponseEntity<Void> deactivateCenter(
            @PathVariable Integer id,
            Authentication authentication) {
        Integer updatedBy = getCurrentUserId(authentication);
        centerService.deactivateCenter(id, updatedBy);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/reactivate")
    @PreAuthorize("@authz.isSuperAdmin(authentication)")
    public ResponseEntity<CenterResponse> reactivateCenter(
            @PathVariable Integer id,
            Authentication authentication) {
        Integer updatedBy = getCurrentUserId(authentication);
        centerService.reactivateCenter(id, updatedBy);
        return ResponseEntity.ok(centerService.getCenterById(id));
    }

    private Integer getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            // TODO: map từ keycloak sub -> userId nội bộ nếu cần
            return null;
        }
        return null;
    }
}

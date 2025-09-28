package com.example.sis.controller;

import com.example.sis.constants.RoleCodes;
import com.example.sis.dto.center.CreateCenterRequest;
import com.example.sis.dto.center.UpdateCenterRequest;
import com.example.sis.dto.center.CenterResponse;
import com.example.sis.service.CenterService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private CenterService centerService;

    @GetMapping
    @PreAuthorize("hasRole('" + RoleCodes.SUPER_ADMIN + "')")
    public ResponseEntity<List<CenterResponse>> getAllActiveCenters() {
        List<CenterResponse> centers = centerService.getAllActiveCenters();
        return ResponseEntity.ok(centers);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('" + RoleCodes.SUPER_ADMIN + "')")
    public ResponseEntity<List<CenterResponse>> getAllCenters() {
        List<CenterResponse> centers = centerService.getAllCenters();
        return ResponseEntity.ok(centers);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('" + RoleCodes.SUPER_ADMIN + "')")
    public ResponseEntity<CenterResponse> getCenterById(@PathVariable Integer id) {
        CenterResponse center = centerService.getCenterById(id);
        return ResponseEntity.ok(center);
    }

    @PostMapping
    @PreAuthorize("hasRole('" + RoleCodes.SUPER_ADMIN + "')")
    public ResponseEntity<CenterResponse> createCenter(
            @Valid @RequestBody CreateCenterRequest request,
            Authentication authentication) {
        Integer createdBy = getCurrentUserId(authentication);
        CenterResponse center = centerService.createCenter(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(center);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('" + RoleCodes.SUPER_ADMIN + "')")
    public ResponseEntity<CenterResponse> updateCenter(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateCenterRequest request,
            Authentication authentication) {
        Integer updatedBy = getCurrentUserId(authentication);
        CenterResponse center = centerService.updateCenter(id, request, updatedBy);
        return ResponseEntity.ok(center);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('" + RoleCodes.SUPER_ADMIN + "')")
    public ResponseEntity<Void> deactivateCenter(
            @PathVariable Integer id,
            Authentication authentication) {
        Integer updatedBy = getCurrentUserId(authentication);
        centerService.deactivateCenter(id, updatedBy);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('" + RoleCodes.SUPER_ADMIN + "')")
    public ResponseEntity<CenterResponse> reactivateCenter(
            @PathVariable Integer id,
            Authentication authentication) {
        Integer updatedBy = getCurrentUserId(authentication);
        centerService.reactivateCenter(id, updatedBy);
        CenterResponse center = centerService.getCenterById(id);
        return ResponseEntity.ok(center);
    }

    private Integer getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            // TODO: Implement logic to get internal user ID from Keycloak user ID
            // For now, return null or a default value
            return null;
        }
        return null;
    }
}

package com.example.sis.controllers;

import com.example.sis.dtos.program.CreateProgramRequest;
import com.example.sis.dtos.program.ProgramLiteResponse;
import com.example.sis.dtos.program.ProgramResponse;
import com.example.sis.dtos.program.UpdateProgramRequest;
import com.example.sis.models.User;
import com.example.sis.repositories.UserRepository;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.services.ProgramService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/programs")
public class ProgramController {

    private final ProgramService programService;
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;

    public ProgramController(ProgramService programService,
                           UserRoleRepository userRoleRepository,
                           UserRepository userRepository) {
        this.programService = programService;
        this.userRoleRepository = userRoleRepository;
        this.userRepository = userRepository;
    }

    /**
     * Lấy danh sách tất cả chương trình học đang hoạt động
     * Endpoint chính: GET /api/programs
     */
    @GetMapping
    public ResponseEntity<List<ProgramLiteResponse>> getAllActivePrograms() {
        return ResponseEntity.ok(programService.getAllActivePrograms());
    }

    /**
     * Lấy danh sách chương trình học đang hoạt động cho dropdown
     * Endpoint phụ với filter: GET /api/programs/lite?category=...
     */
    @GetMapping("/lite")
    public ResponseEntity<List<ProgramLiteResponse>> getActivePrograms(
            @RequestParam(required = false) String category) {
        if (category != null && !category.isEmpty()) {
            return ResponseEntity.ok(programService.getProgramsByCategory(category));
        }
        return ResponseEntity.ok(programService.getAllActivePrograms());
    }

    /**
     * Tạo mới chương trình học
     * POST /api/programs
     */
    @PostMapping
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'TRAINING_MANAGER')")
    public ResponseEntity<ProgramResponse> createProgram(
            @Valid @RequestBody CreateProgramRequest request,
            Authentication authentication) {
        
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        ProgramResponse response = programService.createProgram(request, currentUser);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Cập nhật chương trình học
     * PUT /api/programs/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'TRAINING_MANAGER')")
    public ResponseEntity<ProgramResponse> updateProgram(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateProgramRequest request,
            Authentication authentication) {
        
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        ProgramResponse response = programService.updateProgram(id, request, currentUser);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Xóa chương trình học (soft delete)
     * DELETE /api/programs/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'TRAINING_MANAGER')")
    public ResponseEntity<Void> deleteProgram(
            @PathVariable Integer id,
            Authentication authentication) {
        
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        programService.deleteProgram(id, currentUser);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Lấy User ID hiện tại từ JWT token
     */
    private Integer getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String keycloakUserId = jwt.getSubject();
            return userRoleRepository.findUserIdByKeycloakUserId(keycloakUserId);
        }
        return null;
    }

    /**
     * Lấy User entity hiện tại từ JWT token
     */
    private User getCurrentUser(Authentication authentication) {
        Integer userId = getCurrentUserId(authentication);
        if (userId == null) {
            return null;
        }
        
        return userRepository.findById(userId).orElse(null);
    }
}
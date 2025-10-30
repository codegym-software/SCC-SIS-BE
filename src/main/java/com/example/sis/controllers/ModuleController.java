package com.example.sis.controllers;

import com.example.sis.dtos.module.CreateModuleRequest;
import com.example.sis.dtos.module.ModuleResponse;
import com.example.sis.dtos.module.ReorderModuleRequest;
import com.example.sis.dtos.module.UpdateModuleRequest;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.securities.AuthzService;
import com.example.sis.services.ModuleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý Module/Học phần
 * 
 * Phân quyền:
 * - SUPER_ADMIN: Toàn quyền quản lý modules
 * - TRAINING_MANAGER: Quản lý modules trong chương trình của center mình
 */
@RestController
@RequestMapping("/api/modules")
public class ModuleController {

    private final ModuleService moduleService;
    private final UserRoleRepository userRoleRepository;
    private final AuthzService authzService;

    public ModuleController(ModuleService moduleService,
                            UserRoleRepository userRoleRepository,
                            AuthzService authzService) {
        this.moduleService = moduleService;
        this.userRoleRepository = userRoleRepository;
        this.authzService = authzService;
    }

    /**
     * Tạo module mới
     * 
     * Phân quyền:
     * - Super Admin: Tạo module cho bất kỳ program nào
     * - Training Manager: Tạo module cho programs trong center của mình
     */
    @PostMapping
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'TRAINING_MANAGER')")
    public ResponseEntity<ModuleResponse> createModule(
            @Valid @RequestBody CreateModuleRequest request,
            Authentication authentication) {
        
        Integer createdBy = getCurrentUserId(authentication);
        ModuleResponse response = moduleService.createModule(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lấy danh sách modules của một program
     */
    @GetMapping
    public ResponseEntity<List<ModuleResponse>> getModulesByProgram(
            @RequestParam(required = true) Integer programId,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Boolean mandatoryOnly,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer semester) {
        
        List<ModuleResponse> modules;

        if (q != null && !q.trim().isEmpty()) {
            // Tìm kiếm theo từ khóa
            modules = moduleService.searchModules(programId, q);
        } else if (level != null && !level.trim().isEmpty()) {
            // Lọc theo level
            modules = moduleService.getModulesByLevel(programId, level);
        } else if (mandatoryOnly != null && mandatoryOnly) {
            // Chỉ lấy modules bắt buộc
            modules = moduleService.getMandatoryModules(programId);
        } else if (semester != null) {
            // Lọc theo semester
            modules = moduleService.getModulesBySemester(programId, semester);
        } else {
            // Lấy tất cả
            modules = moduleService.getModulesByProgramId(programId);
        }

        return ResponseEntity.ok(modules);
    }

    /**
     * Lấy chi tiết một module
     */
    @GetMapping("/{moduleId}")
    public ResponseEntity<ModuleResponse> getModuleById(@PathVariable Integer moduleId) {
        ModuleResponse response = moduleService.getModuleById(moduleId);
        return ResponseEntity.ok(response);
    }

    /**
     * Cập nhật module
     * 
     * Lưu ý:
     * - KHÔNG cho phép sửa sequenceOrder và semester
     * - Để thay đổi thứ tự module, sử dụng PATCH /api/modules/reorder
     * 
     * Phân quyền:
     * - Super Admin: Cập nhật bất kỳ module nào
     * - Training Manager: Cập nhật modules trong center của mình
     */
    @PutMapping("/{moduleId}")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'TRAINING_MANAGER')")
    public ResponseEntity<ModuleResponse> updateModule(
            @PathVariable Integer moduleId,
            @Valid @RequestBody UpdateModuleRequest request,
            Authentication authentication) {
        
        Integer updatedBy = getCurrentUserId(authentication);
        ModuleResponse response = moduleService.updateModule(moduleId, request, updatedBy);
        return ResponseEntity.ok(response);
    }

    /**
     * Sắp xếp lại thứ tự module trong program
     * 
     * Endpoint: PATCH /api/modules/reorder?programId={programId}&sequenceOrder={sequenceOrder}
     * 
     * Logic:
     * - Tìm module theo programId và sequenceOrder hiện tại
     * - Di chuyển module đó đến vị trí newSequenceOrder
     * - sequence_order liên tục từ 1 đến n
     * - Semester CỐ ĐỊNH, KHÔNG thay đổi khi reorder
     * - Khi di chuyển module từ vị trí A sang vị trí B, các module khác tự động dịch chuyển
     * 
     * Phân quyền:
     * - Super Admin & Training Manager: Sắp xếp TẤT CẢ modules (kể cả mandatory)
     * - Student: CHỈ sắp xếp modules TỰ CHỌN (optional)
     * 
     * @param programId ID của program chứa module
     * @param sequenceOrder Vị trí hiện tại của module cần di chuyển
     * @param request Request body chứa newSequenceOrder
     */
    @PatchMapping("/reorder")
    @PreAuthorize("isAuthenticated()")  // Cho phép tất cả user đã đăng nhập
    public ResponseEntity<List<ModuleResponse>> reorderModule(
            @RequestParam(required = true) Integer programId,
            @RequestParam(required = true) Integer sequenceOrder,
            @Valid @RequestBody ReorderModuleRequest request,
            Authentication authentication) {
        
        Integer updatedBy = getCurrentUserId(authentication);
        
        // Kiểm tra role: ADMIN/SA có thể sắp xếp tất cả, Student chỉ sắp xếp optional
        boolean isAdminOrSA = authzService.isSuperAdmin(authentication) 
                || authzService.hasRole(authentication, "TRAINING_MANAGER")
                || authzService.hasRole(authentication, "ACADEMIC_STAFF");
        
        List<ModuleResponse> modules = moduleService.reorderModuleBySequenceOrder(
            programId,
            sequenceOrder,
            request.getNewSequenceOrder(), 
            updatedBy,
            isAdminOrSA
        );
        return ResponseEntity.ok(modules);
    }

    /**
     * Gắn tài liệu (resourceUrl) vào module
     * 
     * Endpoint: PUT /api/modules/{moduleId}/resource
     * 
     * Phân quyền:
     * - Super Admin: Gắn tài liệu cho bất kỳ module nào
     * - Training Manager: Gắn tài liệu cho modules trong center của mình
     * 
     * @param moduleId ID của module
     * @param request Request body chứa resourceUrl
     */
    @PutMapping("/{moduleId}/resource")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'TRAINING_MANAGER')")
    public ResponseEntity<ModuleResponse> attachResource(
            @PathVariable Integer moduleId,
            @Valid @RequestBody com.example.sis.dtos.module.AttachResourceRequest request,
            Authentication authentication) {
        
        Integer updatedBy = getCurrentUserId(authentication);
        ModuleResponse response = moduleService.attachResource(
            moduleId, 
            request.getResourceUrl(), 
            updatedBy
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Xóa tài liệu khỏi module
     * - Nếu có query param ?url=... → Xóa TỪNG resource cụ thể
     * - Nếu không có query param → Xóa TOÀN BỘ resources (set null)
     * 
     * Endpoint: DELETE /api/modules/{moduleId}/resource
     * Endpoint: DELETE /api/modules/{moduleId}/resource?url={resourceUrl}
     * 
     * Phân quyền:
     * - Super Admin: Xóa tài liệu của bất kỳ module nào
     * - Training Manager: Xóa tài liệu của modules trong center của mình
     * 
     * @param moduleId ID của module
     * @param url (Optional) URL của tài liệu cần xóa. Nếu null thì xóa hết
     */
    @DeleteMapping("/{moduleId}/resource")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'TRAINING_MANAGER')")
    public ResponseEntity<ModuleResponse> removeResource(
            @PathVariable Integer moduleId,
            @RequestParam(required = false) String url,
            Authentication authentication) {
        
        Integer updatedBy = getCurrentUserId(authentication);
        
        ModuleResponse response;
        if (url != null && !url.isEmpty()) {
            // Xóa TỪNG resource theo URL
            response = moduleService.removeResourceByUrl(moduleId, url, updatedBy);
        } else {
            // Xóa TOÀN BỘ resources
            response = moduleService.removeResource(moduleId, updatedBy);
        }
        
        return ResponseEntity.ok(response);
    }

    // ===== Helper Methods =====

    /**
     * Lấy User ID của người dùng hiện tại từ JWT
     */
    private Integer getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String keycloakUserId = jwt.getSubject(); // Lấy sub claim từ JWT
            
            // Tìm user ID trong database dựa trên keycloak_user_id
            Integer userId = userRoleRepository.findUserIdByKeycloakUserId(keycloakUserId);
            return userId;
        }
        return null;
    }
}


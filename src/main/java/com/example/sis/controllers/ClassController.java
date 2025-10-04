package com.example.sis.controllers;

import com.example.sis.dtos.classes.ClassLiteResponse;
import com.example.sis.dtos.classes.ClassResponse;
import com.example.sis.dtos.classes.CreateClassRequest;
import com.example.sis.dtos.program.ProgramLiteResponse;
import com.example.sis.models.ClassEntity;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.services.ClassService;
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
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService classService;
    private final ProgramService programService;
    private final UserRoleRepository userRoleRepository;

    public ClassController(ClassService classService, ProgramService programService,
            UserRoleRepository userRoleRepository) {
        this.classService = classService;
        this.programService = programService;
        this.userRoleRepository = userRoleRepository;
    }

    /**
     * Tạo lớp học mới
     * Chỉ Super Admin hoặc Academic Staff/Center Manager tại trung tâm đó mới được
     * tạo
     */
    @PostMapping
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'ACADEMIC_STAFF')")
    public ResponseEntity<ClassResponse> createClass(
            @Valid @RequestBody CreateClassRequest request,
            Authentication authentication) {
        Integer createdBy = getCurrentUserId(authentication);

        // Lấy centerId từ user hiện tại
        Integer centerId = getCurrentUserCenterId(authentication);
        if (centerId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .build(); // User không thuộc trung tâm nào
        }

        ClassResponse classResponse = classService.createClass(request, centerId, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(classResponse);
    }

    /**
     * Lấy danh sách tất cả lớp học
     * Super Admin: xem tất cả
     * Academic Staff/Center Manager: chỉ xem lớp trong trung tâm của mình
     */
    @GetMapping
    public ResponseEntity<List<ClassResponse>> getAllClasses(
            @RequestParam(required = false) Integer centerId,
            @RequestParam(required = false) String status,
            Authentication authentication) {

        // Nếu là Super Admin, có thể xem tất cả hoặc filter theo centerId
        if (isCurrentUserSuperAdmin(authentication)) {
            if (centerId != null && status != null) {
                ClassEntity.ClassStatus classStatus = ClassEntity.ClassStatus.valueOf(status.toUpperCase());
                return ResponseEntity.ok(classService.getClassesByCenterAndStatus(centerId, classStatus));
            } else if (centerId != null) {
                return ResponseEntity.ok(classService.getClassesByCenter(centerId));
            } else if (status != null) {
                ClassEntity.ClassStatus classStatus = ClassEntity.ClassStatus.valueOf(status.toUpperCase());
                return ResponseEntity.ok(classService.getClassesByStatus(classStatus));
            } else {
                return ResponseEntity.ok(classService.getAllClasses());
            }
        } else {
            // Academic Staff/Center Manager chỉ xem được lớp trong trung tâm của mình
            Integer userCenterId = getCurrentUserCenterId(authentication);
            if (userCenterId == null) {
                return ResponseEntity.ok(List.of());
            }

            if (status != null) {
                ClassEntity.ClassStatus classStatus = ClassEntity.ClassStatus.valueOf(status.toUpperCase());
                return ResponseEntity.ok(classService.getClassesByCenterAndStatus(userCenterId, classStatus));
            } else {
                return ResponseEntity.ok(classService.getClassesByCenter(userCenterId));
            }
        }
    }

    /**
     * Lấy lớp học theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClassResponse> getClassById(@PathVariable Integer id, Authentication authentication) {
        ClassResponse classResponse = classService.getClassById(id);

        // Kiểm tra quyền truy cập
        if (!isCurrentUserSuperAdmin(authentication)) {
            Integer userCenterId = getCurrentUserCenterId(authentication);
            if (userCenterId == null || !userCenterId.equals(classResponse.getCenterId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        return ResponseEntity.ok(classResponse);
    }

    /**
     * Lấy danh sách lớp học lite cho dropdown
     */
    @GetMapping("/lite")
    public ResponseEntity<List<ClassLiteResponse>> getClassesLite(Authentication authentication) {
        if (isCurrentUserSuperAdmin(authentication)) {
            return ResponseEntity.ok(classService.getClassesLite());
        } else {
            // Academic Staff chỉ xem lớp trong trung tâm của mình
            Integer userCenterId = getCurrentUserCenterId(authentication);
            if (userCenterId == null) {
                return ResponseEntity.ok(List.of());
            }
            List<ClassResponse> classes = classService.getClassesByCenter(userCenterId);
            List<ClassLiteResponse> liteClasses = classes.stream()
                    .map(c -> new ClassLiteResponse(c.getClassId(), c.getName(),
                            c.getProgramName(), c.getCenterName(), c.getStatus()))
                    .toList();
            return ResponseEntity.ok(liteClasses);
        }
    }

    /**
     * Lấy danh sách chương trình học để tạo lớp
     */
    @GetMapping("/programs")
    public ResponseEntity<List<ProgramLiteResponse>> getActivePrograms() {
        return ResponseEntity.ok(programService.getAllActivePrograms());
    }

    /**
     * Lấy User ID hiện tại từ JWT token
     */
    private Integer getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            // TODO: map từ keycloak sub -> userId nội bộ nếu cần
            // Tạm thời return 1 cho test
            return 1;
        }
        return null;
    }

    /**
     * Kiểm tra user hiện tại có phải Super Admin không
     */
    private boolean isCurrentUserSuperAdmin(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String sub = jwt.getClaimAsString("sub");
            return userRoleRepository.userHasActiveRoleByKeycloakIdAndRoleCode(sub, "SUPER_ADMIN");
        }
        return false;
    }

    /**
     * Lấy Center ID của user hiện tại (dành cho Academic Staff/Center Manager)
     */
    private Integer getCurrentUserCenterId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String sub = jwt.getClaimAsString("sub");
            return userRoleRepository.findCenterIdByKeycloakUserId(sub);
        }
        return null;
    }
}
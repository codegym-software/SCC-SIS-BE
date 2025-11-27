package com.example.sis.controllers;

import com.example.sis.dtos.classes.ClassLiteResponse;
import com.example.sis.dtos.classes.ClassResponse;
import com.example.sis.dtos.classes.CreateClassRequest;
import com.example.sis.dtos.classes.UpdateClassRequest;
import com.example.sis.models.ClassEntity;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.services.ClassService;
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
    private final UserRoleRepository userRoleRepository;

    public ClassController(ClassService classService, UserRoleRepository userRoleRepository) {
        this.classService = classService;
        this.userRoleRepository = userRoleRepository;
    }

    /**
     * Tạo lớp học mới
     * - Super Admin: có thể tạo lớp cho bất kỳ trung tâm nào, centerId lấy từ
     * request body
     * - Academic Staff: chỉ tạo lớp cho trung tâm của mình, centerId tự động lấy từ
     * user hiện tại
     */
    @PostMapping
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'ACADEMIC_STAFF')")
    public ResponseEntity<ClassResponse> createClass(
            @Valid @RequestBody CreateClassRequest request,
            Authentication authentication) {
        Integer createdBy = getCurrentUserId(authentication);

        Integer centerId;

        // Nếu là Super Admin, lấy centerId từ request body
        if (isCurrentUserSuperAdmin(authentication)) {
            if (request.getCenterId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            centerId = request.getCenterId();
        } else {
            // Nếu là Academic Staff, tự động lấy centerId từ user hiện tại
            centerId = getCurrentUserCenterId(authentication);
            if (centerId == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            // Override centerId từ request body bằng centerId của user
            request.setCenterId(centerId);
        }

        ClassResponse classResponse = classService.createClass(request, centerId, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(classResponse);
    }

    /**
     * Lấy danh sách tất cả lớp học
     * Super Admin: xem tất cả
     * Academic Staff/Center Manager: chỉ xem lớp trong trung tâm của mình
     * Lecturer: chỉ xem lớp được phân công
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
        } else if (isCurrentUserLecturer(authentication)) {
            // Lecturer chỉ xem được lớp mà họ được phân công
            Integer currentUserId = getCurrentUserId(authentication);
            if (currentUserId == null) {
                return ResponseEntity.ok(List.of());
            }
            
            List<ClassResponse> classes = classService.getClassesByLecturer(currentUserId);
            
            // Filter theo status nếu có
            if (status != null) {
                ClassEntity.ClassStatus classStatus = ClassEntity.ClassStatus.valueOf(status.toUpperCase());
                classes = classes.stream()
                        .filter(c -> c.getStatus().equals(classStatus.name()))
                        .toList();
            }
            
            return ResponseEntity.ok(classes);
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
     * - Super Admin: xem tất cả
     * - Academic Staff: xem lớp trong trung tâm của mình
     * - Lecturer: xem lớp được phân công
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClassResponse> getClassById(@PathVariable Integer id, Authentication authentication) {
        ClassResponse classResponse = classService.getClassById(id);
        Integer currentUserId = getCurrentUserId(authentication);

        // Kiểm tra quyền truy cập
        if (!isCurrentUserSuperAdmin(authentication)) {
            // Kiểm tra nếu là giảng viên
            if (isCurrentUserLecturer(authentication)) {
                // Giảng viên chỉ xem được lớp mà họ được phân công
                boolean isAssigned = classService.isLecturerAssignedToClass(currentUserId, id);
                if (!isAssigned) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            } else {
                // Academic Staff chỉ xem lớp trong trung tâm của mình
                Integer userCenterId = getCurrentUserCenterId(authentication);
                if (userCenterId == null || !userCenterId.equals(classResponse.getCenterId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }
        }

        return ResponseEntity.ok(classResponse);
    }

    /**
     * Lấy danh sách lớp học lite cho dropdown
     * Super Admin: xem tất cả
     * Lecturer: chỉ xem lớp được phân công
     * Academic Staff: xem lớp trong trung tâm của mình
     */
    @GetMapping("/lite")
    public ResponseEntity<List<ClassLiteResponse>> getClassesLite(Authentication authentication) {
        if (isCurrentUserSuperAdmin(authentication)) {
            return ResponseEntity.ok(classService.getClassesLite());
        } else if (isCurrentUserLecturer(authentication)) {
            // Lecturer chỉ xem lớp được phân công
            Integer currentUserId = getCurrentUserId(authentication);
            if (currentUserId == null) {
                return ResponseEntity.ok(List.of());
            }
            List<ClassResponse> classes = classService.getClassesByLecturer(currentUserId);
            List<ClassLiteResponse> liteClasses = classes.stream()
                    .map(c -> new ClassLiteResponse(c.getClassId(), c.getName(),
                            c.getProgramName(), c.getCenterName(), c.getStatus()))
                    .toList();
            return ResponseEntity.ok(liteClasses);
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
     * Cập nhật thông tin lớp học
     * Chỉ Super Admin hoặc Academic Staff tại trung tâm đó mới được cập nhật
     */
    @PutMapping("/{id}")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'ACADEMIC_STAFF')")
    public ResponseEntity<ClassResponse> updateClass(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateClassRequest request,
            Authentication authentication) {

        // Kiểm tra lớp học có tồn tại không
        ClassResponse existingClass = classService.getClassById(id);

        // Kiểm tra quyền truy cập - Academic Staff chỉ được sửa lớp trong center của
        // mình
        if (!isCurrentUserSuperAdmin(authentication)) {
            Integer userCenterId = getCurrentUserCenterId(authentication);
            if (userCenterId == null || !userCenterId.equals(existingClass.getCenterId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        Integer updatedBy = getCurrentUserId(authentication);
        ClassResponse updatedClass = classService.updateClass(id, request, updatedBy);
        return ResponseEntity.ok(updatedClass);
    }

    /**
     * Xóa lớp học (soft delete)
     * Chỉ Super Admin hoặc Academic Staff tại trung tâm đó mới được xóa
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'ACADEMIC_STAFF')")
    public ResponseEntity<?> deleteClass(
            @PathVariable Integer id,
            Authentication authentication) {

        // Kiểm tra lớp học có tồn tại không
        ClassResponse existingClass = classService.getClassById(id);

        // Kiểm tra quyền truy cập - Academic Staff chỉ được xóa lớp trong center của mình
        if (!isCurrentUserSuperAdmin(authentication)) {
            Integer userCenterId = getCurrentUserCenterId(authentication);
            if (userCenterId == null || !userCenterId.equals(existingClass.getCenterId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        Integer deletedBy = getCurrentUserId(authentication);
        classService.deleteClass(id, deletedBy);
        return ResponseEntity.ok("Xóa lớp học thành công");
    }

    /**
     * Lấy User ID hiện tại từ JWT token
     */
    private Integer getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String keycloakUserId = jwt.getSubject(); // Lấy sub claim từ JWT
            System.out.println("Debug - Keycloak User ID from JWT: " + keycloakUserId);

            // Tìm user ID trong database dựa trên keycloak_user_id
            Integer userId = userRoleRepository.findUserIdByKeycloakUserId(keycloakUserId);
            System.out.println("Debug - Found User ID in DB: " + userId);

            return userId;
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
     * Kiểm tra user hiện tại có phải Lecturer không
     */
    private boolean isCurrentUserLecturer(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String sub = jwt.getClaimAsString("sub");
            return userRoleRepository.userHasActiveRoleByKeycloakIdAndRoleCode(sub, "LECTURER");
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

    /**
     * Lấy danh sách lớp học mà giảng viên được phân công
     * Chỉ giảng viên mới được gọi endpoint này
     */
    @GetMapping("/my-classes")
    @PreAuthorize("@authz.hasRole(authentication, 'LECTURER')")
    public ResponseEntity<List<ClassResponse>> getMyClasses(Authentication authentication) {
        Integer currentUserId = getCurrentUserId(authentication);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<ClassResponse> classes = classService.getClassesByLecturer(currentUserId);
        return ResponseEntity.ok(classes);
    }
}
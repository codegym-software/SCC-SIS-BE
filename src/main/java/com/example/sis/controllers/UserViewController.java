package com.example.sis.controllers;

import com.example.sis.constants.RoleCodes;
import com.example.sis.dtos.user.UserViewResponse;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.services.UserViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserViewController {

    private final UserViewService userViewService;
    private final UserRoleRepository userRoleRepository;

    @Autowired
    public UserViewController(UserViewService userViewService,
                              UserRoleRepository userRoleRepository) {
        this.userViewService = userViewService;
        this.userRoleRepository = userRoleRepository;
    }

    /**
     * GET /api/user-views?centerId=&roleCode=&q=
     *
     * - SA: cho phép xem toàn hệ thống (centerId có thể null).
     * - Non-SA: bắt buộc truyền centerId và phải có quyền tại center đó
     *   (CENTER_MANAGER hoặc ACADEMIC_STAFF) mới được xem.
     */
    @GetMapping("/user-views")
    public ResponseEntity<List<UserViewResponse>> searchUserViews(
            @RequestParam(required = false) Integer centerId,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) String q,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String sub = jwt.getSubject();

        boolean isSA = isSuperAdmin(sub);
        if (!isSA) {
            if (centerId == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Non-SA phải truyền centerId để xem trong phạm vi trung tâm.");
            }
            if (!hasCenterAccess(sub, centerId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Bạn không có quyền truy cập dữ liệu trung tâm này.");
            }
        }

        List<UserViewResponse> result = userViewService.search(centerId, roleCode, q);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/user-stats/roles?centerId=
     *
     * - SA: có thể bỏ centerId để xem toàn hệ thống.
     * - Non-SA: bắt buộc centerId và có quyền tại center đó.
     *
     * Response ví dụ: { "LECTURER": 123, "ACADEMIC_STAFF": 45 }
     */
    @GetMapping("/user-stats/roles")
    public ResponseEntity<Map<String, Long>> statsByRole(
            @RequestParam(required = false) Integer centerId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String sub = jwt.getSubject();

        boolean isSA = isSuperAdmin(sub);
        if (!isSA) {
            if (centerId == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Non-SA phải truyền centerId để xem thống kê theo trung tâm.");
            }
            if (!hasCenterAccess(sub, centerId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Bạn không có quyền xem thống kê của trung tâm này.");
            }
        }

        Map<String, Long> result = userViewService.countByRole(centerId);
        return ResponseEntity.ok(result);
    }

    // -------------------- helpers --------------------

    private boolean isSuperAdmin(String keycloakUserId) {
        return userRoleRepository.userHasActiveRoleByKeycloakIdAndRoleCode(
                keycloakUserId, RoleCodes.SUPER_ADMIN
        );
    }

    private boolean hasCenterAccess(String keycloakUserId, Integer centerId) {
        // Quy ước: những ai có quyền xem danh sách trong center:
        // CENTER_MANAGER, ACADEMIC_STAFF (có thể mở rộng thêm tùy chính sách)
        return userRoleRepository.userHasAnyActiveRoleAtCenter(
                keycloakUserId,
                List.of(RoleCodes.CENTER_MANAGER, RoleCodes.ACADEMIC_STAFF),
                centerId
        );
    }
}

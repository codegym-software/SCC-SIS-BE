package com.example.sis.controllers;

import com.example.sis.dtos.user.UserViewResponse;
import com.example.sis.services.UserViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserViewController {

    private final UserViewService userViewService;

    @Autowired
    public UserViewController(UserViewService userViewService) {
        this.userViewService = userViewService;
    }

    /**
     * GET /api/user-views?centerId=&roleCode=&q=
     *
     * - SA: cho phép xem toàn hệ thống (centerId có thể null).
     * - Non-SA: bắt buộc truyền centerId và phải có quyền tại center đó
     *   (CENTER_MANAGER hoặc ACADEMIC_STAFF) mới được xem.
     */
    @GetMapping("/user-views")
    @PreAuthorize("@authz.canListUsers(authentication, #centerId)")
    public ResponseEntity<List<UserViewResponse>> searchUserViews(
            @RequestParam(required = false) Integer centerId,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) String q
    ) {
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
    @PreAuthorize("@authz.hasCenterAccess(authentication, #centerId)")
    public ResponseEntity<Map<String, Long>> statsByRole(
            @RequestParam(required = false) Integer centerId
    ) {
        Map<String, Long> result = userViewService.countByRole(centerId);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/user-views/{userId}
     *
     * Lấy thông tin chi tiết của một user kèm danh sách assignments.
     * Auth: cùng chính sách với GET /api/user-views (ai xem list được thì xem chi tiết được).
     *
     * @param userId ID của user cần lấy thông tin
     * @return UserViewResponse với thông tin user và assignments[]
     */
    @GetMapping("/user-views/{userId}")
    @PreAuthorize("@authz.canListUsers(authentication, null)")
    public ResponseEntity<UserViewResponse> getUserViewById(
            @PathVariable Integer userId
    ) {
        UserViewResponse result = userViewService.findUserView(userId);
        return ResponseEntity.ok(result);
    }
}

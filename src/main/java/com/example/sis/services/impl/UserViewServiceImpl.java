package com.example.sis.services.impl;

import com.example.sis.enums.RoleScope;
import com.example.sis.dtos.user.AssignmentItemResponse;
import com.example.sis.dtos.user.UserAssignmentRow;
import com.example.sis.dtos.user.UserViewResponse;
import com.example.sis.exceptions.ResourceNotFoundException;
import com.example.sis.repositories.UserViewRepository;
import com.example.sis.services.UserViewService;
import com.example.sis.utils.RoleScopeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service tập trung cho API "view" người dùng:
 * - Gộp (user + assignments) từ projection phẳng.
 * - Tính scope GLOBAL/CENTER từ roleCode qua RoleScopeUtil.
 * - Thống kê số user theo roleCode.
 */
@Service
public class UserViewServiceImpl implements UserViewService {

    private final UserViewRepository userViewRepository;

    @Autowired
    public UserViewServiceImpl(UserViewRepository userViewRepository) {
        this.userViewRepository = userViewRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserViewResponse> search(Integer centerId, String roleCode, String q) {
        // chuẩn bị searchPattern (WHERE gọn, tránh concat trong JPQL)
        String searchPattern = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
        // các role GLOBAL (center=null) vẫn phải hiển thị khi lọc theo center
        List<String> globalRoles = List.of("SUPER_ADMIN", "TRAINING_MANAGER");

        // Truy vấn phẳng từ repo (đã đổi chữ ký để nhận searchPattern + globalRoles)
        List<UserAssignmentRow> rows =
                userViewRepository.searchUserViews(centerId, roleCode, searchPattern, globalRoles);

        // Gộp theo userId
        Map<Integer, UserViewResponse> byUser = new LinkedHashMap<>();
        for (UserAssignmentRow r : rows) {
            UserViewResponse u = byUser.get(r.getUserId());
            if (u == null) {
                u = new UserViewResponse();
                u.setUserId(r.getUserId());
                u.setFullName(r.getFullName());
                u.setEmail(r.getEmail());
                u.setPhone(r.getPhone());
                u.setActive(r.isActive());
                u.setSpecialty(r.getSpecialty());
                byUser.put(r.getUserId(), u);
            }

            // Nếu hàng có role (có thể null khi LEFT JOIN)
            if (r.getRoleId() != null && r.getRoleCode() != null) {
                RoleScope scope = RoleScopeUtil.isExclusiveGlobal(r.getRoleCode())
                        ? RoleScope.GLOBAL
                        : RoleScope.CENTER;

                AssignmentItemResponse item = new AssignmentItemResponse(
                        r.getUserRoleId(), // assignmentId để FE dùng hủy gán vai trò
                        r.getRoleId(),
                        r.getRoleCode(),
                        r.getRoleName(),
                        scope,
                        r.getCenterId(),   // null nếu GLOBAL
                        r.getCenterName(), // null nếu GLOBAL
                        r.getAssignedAt()  // ISO-8601 từ user_roles.assignedAt
                );
                u.getAssignments().add(item);
            }
        }

        return new ArrayList<>(byUser.values());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByRole(Integer centerId) {
        // đếm theo CENTER, vẫn giữ GLOBAL khi lọc centerId
        String searchPattern = null;
        String roleCode = null;
        List<String> globalRoles = List.of("SUPER_ADMIN", "TRAINING_MANAGER");

        List<UserAssignmentRow> rows =
                userViewRepository.searchUserViews(centerId, roleCode, searchPattern, globalRoles);

        // roleCode -> set userId (distinct theo user)
        Map<String, Set<Integer>> roleToUsers = new HashMap<>();
        for (UserAssignmentRow r : rows) {
            if (r.getRoleCode() == null) continue; // user chưa có role
            roleToUsers
                    .computeIfAbsent(r.getRoleCode(), k -> new HashSet<>())
                    .add(r.getUserId());
        }

        Map<String, Long> result = new HashMap<>();
        for (Map.Entry<String, Set<Integer>> e : roleToUsers.entrySet()) {
            result.put(e.getKey(), (long) e.getValue().size());
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public UserViewResponse findUserView(Integer userId) {
        // Tạo danh sách global roles để xác định scope
        List<String> globalRoles = List.of("SUPER_ADMIN", "TRAINING_MANAGER");

        // Tìm kiếm với điều kiện chỉ lấy user có userId cụ thể
        List<UserAssignmentRow> rows = userViewRepository.searchUserViews(null, null, null, globalRoles);

        // Lọc chỉ lấy user có userId cần tìm
        List<UserAssignmentRow> userRows = rows.stream()
                .filter(r -> r.getUserId().equals(userId))
                .toList();

        if (userRows.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy thông tin người dùng với ID: " + userId);
        }

        // Tạo UserViewResponse từ hàng đầu tiên (chứa thông tin user cơ bản)
        UserAssignmentRow firstRow = userRows.get(0);
        UserViewResponse userView = new UserViewResponse();
        userView.setUserId(firstRow.getUserId());
        userView.setFullName(firstRow.getFullName());
        userView.setEmail(firstRow.getEmail());
        userView.setPhone(firstRow.getPhone());
        userView.setActive(firstRow.isActive());
        userView.setSpecialty(firstRow.getSpecialty());

        // Thêm các assignments từ tất cả các hàng
        for (UserAssignmentRow r : userRows) {
            if (r.getRoleId() != null && r.getRoleCode() != null) {
                RoleScope scope = RoleScopeUtil.isExclusiveGlobal(r.getRoleCode())
                        ? RoleScope.GLOBAL
                        : RoleScope.CENTER;

                AssignmentItemResponse item = new AssignmentItemResponse(
                        r.getUserRoleId(), // assignmentId để FE dùng hủy gán vai trò
                        r.getRoleId(),
                        r.getRoleCode(),
                        r.getRoleName(),
                        scope,
                        r.getCenterId(),   // null nếu GLOBAL
                        r.getCenterName(), // null nếu GLOBAL
                        r.getAssignedAt()  // ISO-8601 từ user_roles.assignedAt
                );
                userView.getAssignments().add(item);
            }
        }

        return userView;
    }
}

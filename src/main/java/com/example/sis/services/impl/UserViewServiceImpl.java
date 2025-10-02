package com.example.sis.services.impl;

import com.example.sis.enums.RoleScope;
import com.example.sis.dtos.user.AssignmentItemResponse;
import com.example.sis.dtos.user.UserAssignmentRow;
import com.example.sis.dtos.user.UserViewResponse;
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
        // Truy vấn phẳng từ repo
        List<UserAssignmentRow> rows = userViewRepository.searchUserViews(centerId, roleCode, q);

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
                        r.getRoleId(),
                        r.getRoleCode(),
                        r.getRoleName(),
                        scope,
                        r.getCenterId(),    // null nếu GLOBAL
                        r.getCenterName()   // null nếu GLOBAL
                );
                u.getAssignments().add(item);
            }
        }

        return new ArrayList<>(byUser.values());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByRole(Integer centerId) {
        // Lấy toàn bộ rows trong phạm vi center (nếu có), rồi đếm distinct user theo roleCode.
        List<UserAssignmentRow> rows = userViewRepository.searchUserViews(centerId, null, null);

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
}

package com.example.sis.services;

import com.example.sis.dtos.user.UserViewResponse;
import com.example.sis.exceptions.ResourceNotFoundException;

import java.util.List;
import java.util.Map;

public interface UserViewService {

    /**
     * Tìm danh sách user kèm assignments (role+center) phục vụ UI.
     *
     * @param centerId  lọc theo trung tâm (nullable)
     * @param roleCode  lọc theo mã vai trò (nullable), ví dụ: "LECTURER", "ACADEMIC_STAFF"
     * @param q         từ khóa tìm kiếm (nullable) – áp dụng cho fullName/email
     * @return danh sách đã group theo user, mỗi user có mảng assignments[]
     */
    List<UserViewResponse> search(Integer centerId, String roleCode, String q);

    /**
     * Thống kê số user theo roleCode (distinct theo user) trong phạm vi center (nếu có).
     * Trả về map { roleCode -> count }.
     *
     * @param centerId  lọc theo trung tâm (nullable)
     */
    Map<String, Long> countByRole(Integer centerId);

    /**
     * Tìm thông tin chi tiết của một user kèm assignments (role+center) phục vụ UI.
     * Trả về một UserViewResponse duy nhất (không phải list).
     *
     * @param userId ID của user cần tìm
     * @return UserViewResponse với thông tin user và assignments[]
     * @throws ResourceNotFoundException nếu không tìm thấy user
     */
    UserViewResponse findUserView(Integer userId);
}

package com.example.sis.repositories;

import com.example.sis.dtos.user.UserAssignmentRow;
import com.example.sis.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository chuyên cho các truy vấn "view" (phục vụ UI):
 * - Lấy danh sách user kèm (role, center) đã join sẵn.
 * - KHÔNG làm mutate, KHÔNG thay UserRepository/UserRoleRepository hiện có.
 */
public interface UserViewRepository extends JpaRepository<User, Integer> {

    /**
     * Truy vấn phẳng: mỗi hàng là (user + 1 assignment).
     * Service sẽ group theo userId để build assignments[].
     *
     * Lọc:
     *  - centerId: nếu null thì không lọc theo trung tâm.
     *  - roleCode: nếu null thì không lọc theo vai trò.
     *  - q: nếu null thì không search; nếu có, search theo fullName/email.
     *
     * Ghi chú:
     *  - LEFT JOIN để vẫn lấy được user kể cả khi chưa có role.
     *  - ur.revokedAt IS NULL để chỉ lấy assignment đang còn hiệu lực.
     *  - r.active = true để bỏ role bị vô hiệu hóa.
     */
    @Query("""
  SELECT new com.example.sis.dtos.user.UserAssignmentRow(
      u.userId,
      u.fullName,
      u.email,
      u.phone,
      u.active,
      u.specialty,
      ur.id,
      r.roleId,
      r.code,
      r.name,
      ur.assignedAt,
      c.centerId,
      c.name
  )
  FROM User u
  LEFT JOIN UserRole ur ON ur.user = u AND ur.revokedAt IS NULL
  LEFT JOIN Role r      ON r = ur.role AND r.active = true
  LEFT JOIN Center c    ON c = ur.center
  WHERE u.deletedAt IS NULL
    AND (
          :centerId IS NULL
          OR c.centerId = :centerId
          OR (c IS NULL AND r.code IN (:globalRoles))
        )
    AND (:roleCode IS NULL OR r.code = :roleCode)
    AND (
          :searchPattern IS NULL
          OR LOWER(u.fullName) LIKE :searchPattern
          OR LOWER(u.email)    LIKE :searchPattern
        )
  ORDER BY u.userId DESC
""")
    List<UserAssignmentRow> searchUserViews(@Param("centerId") Integer centerId,
                                            @Param("roleCode") String roleCode,
                                            @Param("searchPattern") String searchPattern,
                                            @Param("globalRoles") List<String> globalRoles);




}


// src/main/java/com/example/sis/service/impl/UserServiceImpl.java
package com.example.sis.services.impl;

import com.example.sis.dtos.user.CreateUserRequest;
import com.example.sis.dtos.user.UserResponse;
import com.example.sis.keycloak.KeycloakAdminClient;
import com.example.sis.enums.GenderType;
import com.example.sis.models.Role;
import com.example.sis.models.User;
import com.example.sis.models.UserRole;
import com.example.sis.repositories.CenterRepository;
import com.example.sis.repositories.RoleRepository;
import com.example.sis.repositories.UserRepository;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.services.UserService;
import com.example.sis.utils.RoleScopeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final int MAX_CENTER_SCOPED_ROWS = 3;

    private final UserRepository userRepo;
    private final UserRoleRepository userRoleRepo;
    private final RoleRepository roleRepo;
    private final CenterRepository centerRepo;
    private final KeycloakAdminClient kcAdmin;

    // đọc từ application.properties: keycloak.admin.default-temp-password=Temp@12345
    @Value("${keycloak.admin.default-temp-password:}")
    private String defaultTempPassword;

    public UserServiceImpl(UserRepository userRepo,
                           UserRoleRepository userRoleRepo,
                           RoleRepository roleRepo,
                           CenterRepository centerRepo,
                           KeycloakAdminClient kcAdmin) {
        this.userRepo = userRepo;
        this.userRoleRepo = userRoleRepo;
        this.roleRepo = roleRepo;
        this.centerRepo = centerRepo;
        this.kcAdmin = kcAdmin;
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest req) {
        // 1) Email unique trong DB
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }

        // 2) Đảm bảo có user trên Keycloak và lấy ID (sub)
        String username = req.getEmail(); // thường dùng email làm username
        String ensuredKcId = kcAdmin.ensureUserAndGetId(
                req.getKeycloakUserId(), // có thể null
                req.getEmail(),
                username,
                req.getFullName()
        );

        // 2.1) (Cách A) Đặt mật khẩu tạm nếu cấu hình có giá trị
        if (defaultTempPassword != null && !defaultTempPassword.isBlank()) {
            try {
                kcAdmin.setTemporaryPassword(ensuredKcId, defaultTempPassword, true); // true = bắt đổi mật khẩu khi login
            } catch (Exception ex) {
                // không block luồng tạo user nếu đặt mật khẩu tạm lỗi (dev/test cho nhanh)
                log.warn("Không set được mật khẩu tạm trên Keycloak cho userId={}: {}", ensuredKcId, ex.getMessage());
            }
        }

        // 3) keycloak_user_id unique trong DB
        if (userRepo.existsByKeycloakUserId(ensuredKcId)) {
            throw new IllegalArgumentException("Keycloak user đã tồn tại trong DB");
        }

        // 4) Tạo User trong DB
        User u = new User();
        u.setFullName(req.getFullName());
        u.setEmail(req.getEmail());
        u.setPhone(req.getPhone());
        u.setKeycloakUserId(ensuredKcId);
        u.setDob(req.getDob());
        if (req.getGender() != null) {
            u.setGender(GenderType.valueOf(req.getGender()));
        }
        u.setNationalIdNo(req.getNationalIdNo());
        u.setStartDate(req.getStartDate());
        u.setSpecialty(req.getSpecialty());
        u.setExperience(req.getExperience());
        u.setAddressLine(req.getAddressLine());
        u.setProvince(req.getProvince());
        u.setDistrict(req.getDistrict());
        u.setWard(req.getWard());
        u.setEducationLevel(req.getEducationLevel());
        u.setNote(req.getNote());
        u.setActive(true);
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        u = userRepo.save(u);

        // 5) Gán vai trò theo rule “final”
        if (req.getRoles() != null && !req.getRoles().isEmpty()) {
            Set<Integer> roleIds = req.getRoles().stream()
                    .map(CreateUserRequest.RoleAssignment::getRoleId)
                    .collect(Collectors.toSet());
            Map<Integer, Role> roleById = roleRepo.findAllById(roleIds).stream()
                    .collect(Collectors.toMap(Role::getRoleId, r -> r));

            // validate tồn tại
            for (CreateUserRequest.RoleAssignment a : req.getRoles()) {
                if (!roleById.containsKey(a.getRoleId())) {
                    throw new IllegalArgumentException("Role không tồn tại: " + a.getRoleId());
                }
            }

            boolean hasExclusiveGlobal = req.getRoles().stream()
                    .map(a -> roleById.get(a.getRoleId()).getCode())
                    .anyMatch(RoleScopeUtil::isExclusiveGlobal);

            if (hasExclusiveGlobal) {
                if (req.getRoles().size() > 1) {
                    throw new IllegalArgumentException("Role độc quyền (SUPER_ADMIN / TRAINING_MANAGER) không được đi kèm vai trò khác");
                }
                CreateUserRequest.RoleAssignment a = req.getRoles().get(0);
                Role role = roleById.get(a.getRoleId());
                if (a.getCenterId() != null) {
                    throw new IllegalArgumentException(role.getCode() + " là global, centerId phải null");
                }
                if (userRoleRepo.existsActiveAssignment(u.getUserId(), role.getRoleId(), null)) {
                    throw new IllegalArgumentException("Vai trò " + role.getCode() + " đã gán cho user này");
                }
                UserRole ur = new UserRole();
                ur.setUser(u);
                ur.setRole(role);
                ur.setCenter(null);
                ur.setAssignedAt(LocalDateTime.now());
                ur.setCreatedAt(LocalDateTime.now());
                userRoleRepo.save(ur);

            } else {
                // center-scoped only
                for (CreateUserRequest.RoleAssignment a : req.getRoles()) {
                    Role role = roleById.get(a.getRoleId());
                    String code = role.getCode();
                    if (!RoleScopeUtil.isCenterScoped(code)) {
                        throw new IllegalArgumentException("Role chưa được cấu hình scope hợp lệ: " + code);
                    }
                    if (a.getCenterId() == null) {
                        throw new IllegalArgumentException(code + " là center-scoped, centerId không được null");
                    }
                    centerRepo.findById(a.getCenterId())
                            .orElseThrow(() -> new IllegalArgumentException("Center không tồn tại: " + a.getCenterId()));
                }
                if (req.getRoles().size() > MAX_CENTER_SCOPED_ROWS) {
                    throw new IllegalArgumentException("Tối đa " + MAX_CENTER_SCOPED_ROWS + " dòng vai trò center-scoped cho mỗi user");
                }
                for (CreateUserRequest.RoleAssignment a : req.getRoles()) {
                    Role role = roleById.get(a.getRoleId());
                    Integer centerId = a.getCenterId();
                    if (userRoleRepo.existsActiveAssignment(u.getUserId(), role.getRoleId(), centerId)) {
                        throw new IllegalArgumentException("Vai trò bị trùng (user, role, center) đang active: roleId="
                                + role.getRoleId() + ", centerId=" + centerId);
                    }
                    UserRole ur = new UserRole();
                    ur.setUser(u);
                    ur.setRole(role);
                    ur.setCenter(centerRepo.getReferenceById(centerId));
                    ur.setAssignedAt(LocalDateTime.now());
                    ur.setCreatedAt(LocalDateTime.now());
                    userRoleRepo.save(ur);
                }
            }
        }

        return toResponse(u);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getUsers(Integer centerId) {
        List<User> users = userRepo.findUsersByCenterId(centerId);
        return users.stream().map(this::toResponse).toList();
    }

    private UserResponse toResponse(User u) {
        UserResponse res = new UserResponse();
        res.setUserId(u.getUserId());
        res.setFullName(u.getFullName());
        res.setEmail(u.getEmail());
        res.setPhone(u.getPhone());
        res.setKeycloakUserId(u.getKeycloakUserId());
        res.setDob(u.getDob());
        res.setGender(u.getGender() == null ? null : u.getGender().name());
        res.setActive(u.isActive());
        res.setCreatedAt(u.getCreatedAt());
        res.setUpdatedAt(u.getUpdatedAt());
        return res;
    }
}

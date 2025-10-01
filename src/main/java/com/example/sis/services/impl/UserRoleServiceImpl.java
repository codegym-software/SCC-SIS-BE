package com.example.sis.services.impl;

import com.example.sis.dtos.userrole.UserRoleRequest;
import com.example.sis.dtos.userrole.UserRoleResponse;
import com.example.sis.dtos.user.UserResponse;
import com.example.sis.dtos.role.RoleResponse;
import com.example.sis.dtos.center.CenterResponse;
import com.example.sis.models.UserRole;
import com.example.sis.models.Role;
import com.example.sis.models.Center;
import com.example.sis.models.User;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.repositories.RoleRepository;
import com.example.sis.repositories.CenterRepository;
import com.example.sis.repositories.UserRepository;
import com.example.sis.services.UserRoleService;
import com.example.sis.exceptions.BadRequestException;
import com.example.sis.exceptions.NotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserRoleServiceImpl implements UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final CenterRepository centerRepository;
    private final UserRepository userRepository;

    public UserRoleServiceImpl(UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            CenterRepository centerRepository,
            UserRepository userRepository) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.centerRepository = centerRepository;
        this.userRepository = userRepository;
    }

    @Override
    public UserRoleResponse assignRoleToUser(UserRoleRequest request, String assignedBy) {
        // Validate input
        if (request.getUserId() == null) {
            throw new BadRequestException("User ID không được để trống");
        }
        if (request.getRoleId() == null) {
            throw new BadRequestException("Role ID không được để trống");
        }

        // Find entities
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy user với ID: " + request.getUserId()));

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy role với ID: " + request.getRoleId()));

        Center center = null;
        if (request.getCenterId() != null) {
            center = centerRepository.findById(request.getCenterId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy center với ID: " + request.getCenterId()));
        }

        // Check if assignment already exists and is active
        boolean existsActiveAssignment = userRoleRepository.existsActiveAssignment(
                request.getUserId(), request.getRoleId(), request.getCenterId());

        if (existsActiveAssignment) {
            throw new BadRequestException("User đã có role này tại center này");
        }

        // Create new UserRole
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setCenter(center);
        userRole.setAssignedBy(assignedBy);
        userRole.setAssignedAt(LocalDateTime.now());
        userRole.setCreatedAt(LocalDateTime.now());

        userRole = userRoleRepository.save(userRole);
        return convertToResponse(userRole);
    }

    @Override
    public void revokeRoleFromUser(Integer userRoleId, String revokedBy) {
        if (userRoleId == null) {
            throw new BadRequestException("UserRole ID không được để trống");
        }

        UserRole userRole = userRoleRepository.findById(userRoleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy user role với ID: " + userRoleId));

        if (userRole.getRevokedAt() != null) {
            throw new BadRequestException("User role này đã bị thu hồi rồi");
        }

        userRole.setRevokedBy(revokedBy);
        userRole.setRevokedAt(LocalDateTime.now());
        userRoleRepository.save(userRole);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRoleResponse> getUserRolesByCenterId(Integer centerId) {
        if (centerId == null) {
            throw new BadRequestException("Center ID không được để trống");
        }

        List<UserRole> userRoles = userRoleRepository.findActiveByCenterId(centerId);
        return userRoles.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRoleResponse> getUserRolesByUserId(Integer userId) {
        if (userId == null) {
            throw new BadRequestException("User ID không được để trống");
        }

        List<UserRole> userRoles = userRoleRepository.findActiveByUserId(userId);
        return userRoles.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasRoleAtCenter(Integer userId, String roleCode, Integer centerId) {
        if (userId == null) {
            throw new BadRequestException("User ID không được để trống");
        }
        if (roleCode == null || roleCode.trim().isEmpty()) {
            throw new BadRequestException("Role code không được để trống");
        }

        return userRoleRepository.userHasActiveRoleByUserIdAndRoleCodeAndCenterId(
                userId, roleCode, centerId);
    }

    private UserRoleResponse convertToResponse(UserRole userRole) {
        UserResponse userResponse = new UserResponse();
        userResponse.setUserId(userRole.getUser().getUserId());
        userResponse.setFullName(userRole.getUser().getFullName());
        userResponse.setEmail(userRole.getUser().getEmail());
        userResponse.setPhone(userRole.getUser().getPhone());
        userResponse.setKeycloakUserId(userRole.getUser().getKeycloakUserId());
        userResponse.setDob(userRole.getUser().getDob());
        userResponse
                .setGender(userRole.getUser().getGender() != null ? userRole.getUser().getGender().toString() : null);
        userResponse.setActive(userRole.getUser().isActive());
        userResponse.setCreatedAt(userRole.getUser().getCreatedAt());
        userResponse.setUpdatedAt(userRole.getUser().getUpdatedAt());

        RoleResponse roleResponse = new RoleResponse();
        roleResponse.setRoleId(userRole.getRole().getRoleId());
        roleResponse.setCode(userRole.getRole().getCode());
        roleResponse.setName(userRole.getRole().getName());
        roleResponse.setActive(userRole.getRole().isActive());

        CenterResponse centerResponse = null;
        if (userRole.getCenter() != null) {
            centerResponse = new CenterResponse();
            centerResponse.setCenterId(userRole.getCenter().getCenterId());
            centerResponse.setName(userRole.getCenter().getName());
            centerResponse.setCode(userRole.getCenter().getCode());
            centerResponse.setEmail(userRole.getCenter().getEmail());
            centerResponse.setPhone(userRole.getCenter().getPhone());
            centerResponse.setEstablishedDate(userRole.getCenter().getEstablishedDate());
            centerResponse.setDescription(userRole.getCenter().getDescription());
            centerResponse.setAddressLine(userRole.getCenter().getAddressLine());
            centerResponse.setProvince(userRole.getCenter().getProvince());
            centerResponse.setDistrict(userRole.getCenter().getDistrict());
            centerResponse.setWard(userRole.getCenter().getWard());
            centerResponse.setCreatedBy(userRole.getCenter().getCreatedBy());
            centerResponse.setUpdatedBy(userRole.getCenter().getUpdatedBy());
            centerResponse.setCreatedAt(userRole.getCenter().getCreatedAt());
            centerResponse.setUpdatedAt(userRole.getCenter().getUpdatedAt());
            centerResponse.setDeletedAt(userRole.getCenter().getDeletedAt());
        }

        UserRoleResponse response = new UserRoleResponse();
        response.setUserRoleId(userRole.getUserRoleId());
        response.setUser(userResponse);
        response.setRole(roleResponse);
        response.setCenter(centerResponse);
        response.setAssignedAt(userRole.getAssignedAt());
        response.setAssignedBy(userRole.getAssignedBy());
        response.setRevokedAt(userRole.getRevokedAt());
        response.setRevokedBy(userRole.getRevokedBy());

        return response;
    }
}
package com.example.sis.service.impl;

import com.example.sis.dto.center.CreateCenterRequest;
import com.example.sis.dto.center.UpdateCenterRequest;
import com.example.sis.dto.center.CenterResponse;
import com.example.sis.exception.BadRequestException;
import com.example.sis.exception.NotFoundException;
import com.example.sis.model.Center;
import com.example.sis.model.UserRole;
import com.example.sis.repository.CenterRepository;
import com.example.sis.repository.UserRoleRepository;
import com.example.sis.service.CenterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CenterServiceImpl implements CenterService {

    @Autowired
    private CenterRepository centerRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CenterResponse> getAllActiveCenters() {
        List<Center> centers = centerRepository.findAllActiveOrderByCreatedAtDesc();
        return centers.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CenterResponse createCenter(CreateCenterRequest request, Integer createdBy) {
        validateCreateRequest(request);

        Center center = new Center();
        mapRequestToEntity(request, center);
        center.setCreatedBy(createdBy);

        Center savedCenter = centerRepository.save(center);
        return convertToResponse(savedCenter);
    }

    @Override
    public CenterResponse updateCenter(Integer centerId, UpdateCenterRequest request, Integer updatedBy) {
        Center center = centerRepository.findActiveById(centerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trung tâm với ID: " + centerId));

        validateUpdateRequest(request, centerId);

        mapUpdateRequestToEntity(request, center);
        center.setUpdatedBy(updatedBy);

        Center savedCenter = centerRepository.save(center);
        return convertToResponse(savedCenter);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CenterResponse> getAllCenters() {
        List<Center> centers = centerRepository.findAllOrderByCreatedAtDesc();
        return centers.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CenterResponse getCenterById(Integer centerId) {
        Center center = centerRepository.findActiveById(centerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trung tâm với ID: " + centerId));
        return convertToResponse(center);
    }

    @Override
    public void deactivateCenter(Integer centerId, Integer updatedBy) {
        Center center = centerRepository.findActiveById(centerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trung tâm với ID: " + centerId));

        center.setDeletedAt(java.time.LocalDateTime.now());
        center.setUpdatedBy(updatedBy);
        centerRepository.save(center);

        // Thu hồi tất cả user_roles liên quan đến center này
        revokeAllUserRolesForCenter(centerId);
    }

    @Override
    public void reactivateCenter(Integer centerId, Integer updatedBy) {
        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trung tâm với ID: " + centerId));

        if (center.getDeletedAt() == null) {
            throw new BadRequestException("Trung tâm đã đang hoạt động");
        }

        center.setDeletedAt(null);
        center.setUpdatedBy(updatedBy);
        centerRepository.save(center);

        // Restore tất cả user_roles đã bị thu hồi khi center bị vô hiệu hóa
        restoreUserRolesForCenter(centerId);
    }

    private void validateCreateRequest(CreateCenterRequest request) {
        if (centerRepository.existsByCodeAndNotDeleted(request.getCode())) {
            throw new BadRequestException("Mã trung tâm đã tồn tại: " + request.getCode());
        }
        if (centerRepository.existsByEmailAndNotDeleted(request.getEmail())) {
            throw new BadRequestException("Email đã được sử dụng: " + request.getEmail());
        }
    }

    private void validateUpdateRequest(UpdateCenterRequest request, Integer centerId) {
        if (centerRepository.existsByCodeAndNotDeletedAndIdNot(request.getCode(), centerId)) {
            throw new BadRequestException("Mã trung tâm đã tồn tại: " + request.getCode());
        }
        if (centerRepository.existsByEmailAndNotDeletedAndIdNot(request.getEmail(), centerId)) {
            throw new BadRequestException("Email đã được sử dụng: " + request.getEmail());
        }
    }

    private void mapRequestToEntity(CreateCenterRequest request, Center center) {
        center.setName(request.getName());
        center.setCode(request.getCode());
        center.setEmail(request.getEmail());
        center.setPhone(request.getPhone());
        center.setEstablishedDate(request.getEstablishedDate());
        center.setDescription(request.getDescription());
        center.setAddressLine(request.getAddressLine());
        center.setProvince(request.getProvince());
        center.setDistrict(request.getDistrict());
        center.setWard(request.getWard());
    }

    private void mapUpdateRequestToEntity(UpdateCenterRequest request, Center center) {
        center.setName(request.getName());
        center.setCode(request.getCode());
        center.setEmail(request.getEmail());
        center.setPhone(request.getPhone());
        center.setEstablishedDate(request.getEstablishedDate());
        center.setDescription(request.getDescription());
        center.setAddressLine(request.getAddressLine());
        center.setProvince(request.getProvince());
        center.setDistrict(request.getDistrict());
        center.setWard(request.getWard());
    }

    private CenterResponse convertToResponse(Center center) {
        CenterResponse response = new CenterResponse();
        response.setCenterId(center.getCenterId());
        response.setName(center.getName());
        response.setCode(center.getCode());
        response.setEmail(center.getEmail());
        response.setPhone(center.getPhone());
        response.setEstablishedDate(center.getEstablishedDate());
        response.setDescription(center.getDescription());
        response.setAddressLine(center.getAddressLine());
        response.setProvince(center.getProvince());
        response.setDistrict(center.getDistrict());
        response.setWard(center.getWard());
        response.setCreatedBy(center.getCreatedBy());
        response.setUpdatedBy(center.getUpdatedBy());
        response.setCreatedAt(center.getCreatedAt());
        response.setUpdatedAt(center.getUpdatedAt());
        response.setDeletedAt(center.getDeletedAt());
        return response;
    }

    /**
     * Thu hồi tất cả user-roles đang active của một center
     */
    private void revokeAllUserRolesForCenter(Integer centerId) {
        List<UserRole> activeUserRoles = userRoleRepository.findActiveByCenterId(centerId);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        for (UserRole userRole : activeUserRoles) {
            userRole.setRevokedAt(now);
        }

        if (!activeUserRoles.isEmpty()) {
            userRoleRepository.saveAll(activeUserRoles);
            System.out.println("✅ Đã thu hồi " + activeUserRoles.size() + " user-roles cho center ID: " + centerId);
        }
    }

    /**
     * Restore tất cả user_roles đã bị thu hồi cho center
     */
    private void restoreUserRolesForCenter(Integer centerId) {
        List<UserRole> revokedUserRoles = userRoleRepository.findRevokedByCenterId(centerId);

        if (revokedUserRoles.isEmpty()) {
            System.out.println("⚠️ Không có user-roles nào cần restore cho center ID: " + centerId);
            return;
        }

        // Set revokedAt = null để restore user-roles
        for (UserRole userRole : revokedUserRoles) {
            userRole.setRevokedAt(null);
        }

        userRoleRepository.saveAll(revokedUserRoles);
        System.out.println("✅ Đã restore " + revokedUserRoles.size() + " user-roles cho center ID: " + centerId);
    }
}

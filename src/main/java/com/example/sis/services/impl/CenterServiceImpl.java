package com.example.sis.services.impl;

import com.example.sis.dtos.center.CenterLiteResponse;
import com.example.sis.dtos.center.CenterResponse;
import com.example.sis.dtos.center.CreateCenterRequest;
import com.example.sis.dtos.center.UpdateCenterRequest;
import com.example.sis.exceptions.BadRequestException;
import com.example.sis.exceptions.NotFoundException;
import com.example.sis.models.Center;
import com.example.sis.models.UserRole;
import com.example.sis.repositories.CenterRepository;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.services.CenterService;
import com.example.sis.services.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CenterServiceImpl implements CenterService {

    private final CenterRepository centerRepository;
    private final UserRoleRepository userRoleRepository;
    private final NotificationService notificationService;

    public CenterServiceImpl(CenterRepository centerRepository,
                             UserRoleRepository userRoleRepository,
                             NotificationService notificationService) {
        this.centerRepository = centerRepository;
        this.userRoleRepository = userRoleRepository;
        this.notificationService = notificationService;
    }

    // ===== LITE (dropdown) =====
    @Override
    @Transactional(readOnly = true)
    public List<CenterLiteResponse> listCentersLite() {
        // Lấy từ DTO projection từ repository (chỉ select centerId, code, name)
        return centerRepository.findLiteActive();
    }

    // ===== Quản trị (dev1) =====
    @Override
    @Transactional(readOnly = true)
    public List<CenterResponse> getAllActiveCenters() {
        return centerRepository.findAllActiveOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CenterResponse> getAllCenters() {
        return centerRepository.findAllOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CenterResponse getCenterById(Integer centerId) {
        Center c = centerRepository.findActiveById(centerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trung tâm ID: " + centerId));
        return toResponse(c);
    }

    @Override
    public CenterResponse createCenter(CreateCenterRequest req, Integer createdBy) {
        validateCreate(req);
        Center c = new Center();
        applyCreate(req, c);
        c.setCreatedBy(createdBy);
        Center saved = centerRepository.save(c);
        
        // Gửi thông báo cho SUPER_ADMIN về trung tâm mới (bao gồm cả người tạo)
        notificationService.notifyAdminsExcept(
                saved.getCenterId(),
                null, // null = gửi cho tất cả, không loại trừ ai
                "CENTER_CREATED",
                "Trung tâm mới được tạo",
                String.format("Trung tâm %s (%s) đã được tạo", saved.getName(), saved.getCode()),
                "CENTER",
                saved.getCenterId().longValue(),
                "INFO"
        );
        
        return toResponse(saved);
    }

    @Override
    public CenterResponse updateCenter(Integer centerId, UpdateCenterRequest req, Integer updatedBy) {
        Center c = centerRepository.findActiveById(centerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trung tâm ID: " + centerId));
        validateUpdate(req, centerId);
        applyUpdate(req, c);
        c.setUpdatedBy(updatedBy);
        Center saved = centerRepository.save(c);
        return toResponse(saved);
    }

    @Override
    public void deactivateCenter(Integer centerId, Integer updatedBy) {
        Center c = centerRepository.findActiveById(centerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trung tâm ID: " + centerId));
        c.setDeletedAt(LocalDateTime.now());
        c.setUpdatedBy(updatedBy);
        centerRepository.save(c);

        List<UserRole> active = userRoleRepository.findActiveByCenterId(centerId);
        if (!active.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            active.forEach(ur -> ur.setRevokedAt(now));
            userRoleRepository.saveAll(active);
        }
    }

    @Override
    public void reactivateCenter(Integer centerId, Integer updatedBy) {
        Center c = centerRepository.findById(centerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trung tâm ID: " + centerId));
        if (c.getDeletedAt() == null) {
            throw new BadRequestException("Trung tâm đang hoạt động");
        }
        c.setDeletedAt(null);
        c.setUpdatedBy(updatedBy);
        centerRepository.save(c);

        List<UserRole> revoked = userRoleRepository.findRevokedByCenterId(centerId);
        if (!revoked.isEmpty()) {
            revoked.forEach(ur -> ur.setRevokedAt(null));
            userRoleRepository.saveAll(revoked);
        }
    }

    // ===== helpers =====
    private void validateCreate(CreateCenterRequest r) {
        if (centerRepository.existsByCodeAndNotDeleted(r.getCode()))
            throw new BadRequestException("Mã trung tâm đã tồn tại: " + r.getCode());
        if (centerRepository.existsByEmailAndNotDeleted(r.getEmail()))
            throw new BadRequestException("Email đã được sử dụng: " + r.getEmail());
    }

    private void validateUpdate(UpdateCenterRequest r, Integer id) {
        if (centerRepository.existsByCodeAndNotDeletedAndIdNot(r.getCode(), id))
            throw new BadRequestException("Mã trung tâm đã tồn tại: " + r.getCode());
        if (centerRepository.existsByEmailAndNotDeletedAndIdNot(r.getEmail(), id))
            throw new BadRequestException("Email đã được sử dụng: " + r.getEmail());
    }

    private void applyCreate(CreateCenterRequest r, Center c) {
        c.setName(r.getName());
        c.setCode(r.getCode());
        c.setEmail(r.getEmail());
        c.setPhone(r.getPhone());
        c.setEstablishedDate(r.getEstablishedDate());
        c.setDescription(r.getDescription());
        c.setAddressLine(r.getAddressLine());
        c.setProvince(r.getProvince());
        c.setDistrict(r.getDistrict());
        c.setWard(r.getWard());
    }

    private void applyUpdate(UpdateCenterRequest r, Center c) {
        c.setName(r.getName());
        c.setCode(r.getCode());
        c.setEmail(r.getEmail());
        c.setPhone(r.getPhone());
        c.setEstablishedDate(r.getEstablishedDate());
        c.setDescription(r.getDescription());
        c.setAddressLine(r.getAddressLine());
        c.setProvince(r.getProvince());
        c.setDistrict(r.getDistrict());
        c.setWard(r.getWard());
    }

    private CenterResponse toResponse(Center c) {
        CenterResponse res = new CenterResponse();
        res.setCenterId(c.getCenterId());
        res.setName(c.getName());
        res.setCode(c.getCode());
        res.setEmail(c.getEmail());
        res.setPhone(c.getPhone());
        res.setEstablishedDate(c.getEstablishedDate());
        res.setDescription(c.getDescription());
        res.setAddressLine(c.getAddressLine());
        res.setProvince(c.getProvince());
        res.setDistrict(c.getDistrict());
        res.setWard(c.getWard());
        res.setCreatedBy(c.getCreatedBy());
        res.setUpdatedBy(c.getUpdatedBy());
        res.setCreatedAt(c.getCreatedAt());
        res.setUpdatedAt(c.getUpdatedAt());
        res.setDeletedAt(c.getDeletedAt());
        return res;
    }
}

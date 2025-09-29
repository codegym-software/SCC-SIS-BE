package com.example.sis.services;

import com.example.sis.dtos.center.CenterLiteResponse;
import com.example.sis.dtos.center.CenterResponse;
import com.example.sis.dtos.center.CreateCenterRequest;
import com.example.sis.dtos.center.UpdateCenterRequest;

import java.util.List;

public interface CenterService {
    // LITE cho dropdown (không phá FE quản trị)
    List<CenterLiteResponse> listCentersLite();

    // Bộ quản trị (giữ tên/hành vi như dev1)
    List<CenterResponse> getAllActiveCenters();
    List<CenterResponse> getAllCenters();
    CenterResponse getCenterById(Integer centerId);
    CenterResponse createCenter(CreateCenterRequest request, Integer createdBy);
    CenterResponse updateCenter(Integer centerId, UpdateCenterRequest request, Integer updatedBy);
    void deactivateCenter(Integer centerId, Integer updatedBy);
    void reactivateCenter(Integer centerId, Integer updatedBy);
}

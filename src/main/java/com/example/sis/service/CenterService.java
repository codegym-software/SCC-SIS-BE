package com.example.sis.service;

import com.example.sis.dto.center.CreateCenterRequest;
import com.example.sis.dto.center.UpdateCenterRequest;
import com.example.sis.dto.center.CenterResponse;

import java.util.List;

public interface CenterService {
    List<CenterResponse> getAllActiveCenters();

    List<CenterResponse> getAllCenters();

    CenterResponse createCenter(CreateCenterRequest request, Integer createdBy);

    CenterResponse updateCenter(Integer centerId, UpdateCenterRequest request, Integer updatedBy);

    CenterResponse getCenterById(Integer centerId);

    void deactivateCenter(Integer centerId, Integer updatedBy);

    void reactivateCenter(Integer centerId, Integer updatedBy);
}

package com.example.sis.services;

import com.example.sis.dtos.center.CenterResponse;

import java.util.List;

public interface CenterService {
    List<CenterResponse> listCenters();
}

package com.example.sis.services.impl;

import com.example.sis.dtos.center.CenterResponse;
import com.example.sis.models.Center;
import com.example.sis.repositories.CenterRepository;
import com.example.sis.services.CenterService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CenterServiceImpl implements CenterService {

    private final CenterRepository centerRepository;

    public CenterServiceImpl(CenterRepository centerRepository) {
        this.centerRepository = centerRepository;
    }

    @Override
    public List<CenterResponse> listCenters() {
        List<Center> centers = centerRepository.findAllByOrderByNameAsc();
        return centers.stream()
                .map(c -> new CenterResponse(c.getCenterId(), c.getCode(), c.getName()))
                .collect(Collectors.toList());
    }
}

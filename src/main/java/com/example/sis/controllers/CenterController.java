package com.example.sis.controllers;

import com.example.sis.dtos.center.CenterResponse;
import com.example.sis.services.CenterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RestController
@RequestMapping("/api/centers")
public class CenterController {

    private final CenterService centerService;

    public CenterController(CenterService centerService) {
        this.centerService = centerService;
    }

    @GetMapping
    public ResponseEntity<List<CenterResponse>> listCenters() {
        return ResponseEntity.ok(centerService.listCenters());
    }
}

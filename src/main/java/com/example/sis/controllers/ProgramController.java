package com.example.sis.controllers;

import com.example.sis.dtos.program.ProgramLiteResponse;
import com.example.sis.services.ProgramService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/programs")
public class ProgramController {

    private final ProgramService programService;

    public ProgramController(ProgramService programService) {
        this.programService = programService;
    }

    /**
     * Lấy danh sách chương trình học đang hoạt động cho dropdown
     */
    @GetMapping("/lite")
    public ResponseEntity<List<ProgramLiteResponse>> getActivePrograms(
            @RequestParam(required = false) String category) {
        if (category != null && !category.isEmpty()) {
            return ResponseEntity.ok(programService.getProgramsByCategory(category));
        }
        return ResponseEntity.ok(programService.getAllActivePrograms());
    }
}
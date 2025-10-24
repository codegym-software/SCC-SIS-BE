package com.example.sis.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

    // Thư mục lưu file upload (có thể config trong application.properties)
    @Value("${file.upload.dir:uploads/syllabus}")
    private String uploadDir;

    // Base URL để truy cập file (có thể config trong application.properties)
    @Value("${file.base.url:http://localhost:7000/uploads/syllabus}")
    private String baseUrl;

    /**
     * Upload file đề cương (syllabus) cho module
     * POST /api/files/upload/syllabus
     */
    @PostMapping("/upload/syllabus")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadSyllabusFile(@RequestParam("file") MultipartFile file) {
        try {
            // 1. Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File không được để trống"));
            }

            // Validate file size (max 50MB)
            long maxSize = 50 * 1024 * 1024; // 50MB
            if (file.getSize() > maxSize) {
                return ResponseEntity.badRequest().body(Map.of("error", "Kích thước file không được vượt quá 50MB"));
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !isAllowedFileType(contentType)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Chỉ hỗ trợ file PDF, Word, Excel, PowerPoint"));
            }

            // 2. Tạo thư mục nếu chưa tồn tại
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 3. Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // Filename format: timestamp_uuid.ext
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String newFilename = timestamp + "_" + uniqueId + fileExtension;

            // 4. Save file
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath);

            // 5. Generate URL
            String fileUrl = baseUrl + "/" + newFilename;

            // 6. Detect file type
            String fileType = detectFileType(originalFilename, contentType);

            // 7. Return response
            Map<String, Object> response = new HashMap<>();
            response.put("url", fileUrl);
            response.put("fileName", originalFilename);
            response.put("fileSize", file.getSize());
            response.put("fileType", fileType);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Không thể tải lên file: " + e.getMessage()));
        }
    }

    /**
     * Kiểm tra file type có được phép không
     */
    private boolean isAllowedFileType(String contentType) {
        return contentType.equals("application/pdf") ||
                contentType.equals("application/msword") ||
                contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                contentType.equals("application/vnd.ms-excel") ||
                contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
                contentType.equals("application/vnd.ms-powerpoint") ||
                contentType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation");
    }

    /**
     * Detect file type từ filename và content type
     */
    private String detectFileType(String filename, String contentType) {
        if (filename == null) return "FILE";

        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "PDF";
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return "DOCX";
        if (lower.endsWith(".xls") || lower.endsWith(".xlsx")) return "XLSX";
        if (lower.endsWith(".ppt") || lower.endsWith(".pptx")) return "PPTX";

        return "FILE";
    }
}

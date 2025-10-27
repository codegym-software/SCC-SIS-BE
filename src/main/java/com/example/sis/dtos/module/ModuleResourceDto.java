package com.example.sis.dtos.module;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

/**
 * DTO đại diện cho 1 tài liệu học tập (file/link) của module
 * Sẽ được lưu dưới dạng JSON array trong field module.syllabusUrl
 */
public class ModuleResourceDto {

    @JsonProperty("url")
    @NotBlank(message = "URL tài liệu không được để trống")
    @Size(max = 1000, message = "URL không được vượt quá 1000 ký tự")
    @Pattern(regexp = "^(https?://.*)?$", message = "URL phải là URL hợp lệ (http/https)")
    private String url;

    @JsonProperty("fileName")
    private String fileName;

    @JsonProperty("fileType")
    private String fileType; // PDF, DOCX, YOUTUBE, GOOGLE_DRIVE, EXTERNAL_LINK

    @JsonProperty("fileSize")
    private Long fileSize; // bytes

    @JsonProperty("uploadedAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime uploadedAt;

    @JsonProperty("uploadedBy")
    private Integer uploadedBy;

    // ===== Constructors =====

    public ModuleResourceDto() {
    }

    public ModuleResourceDto(String url, String fileName, String fileType, Long fileSize, LocalDateTime uploadedAt, Integer uploadedBy) {
        this.url = url;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uploadedAt = uploadedAt;
        this.uploadedBy = uploadedBy;
    }

    // ===== Getters & Setters =====

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Integer getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Integer uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
}

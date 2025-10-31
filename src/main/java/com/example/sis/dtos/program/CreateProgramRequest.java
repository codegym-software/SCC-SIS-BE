package com.example.sis.dtos.program;

import com.example.sis.models.Program.DeliveryMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * DTO cho request tạo mới Program
 */
public class CreateProgramRequest {

    @NotBlank(message = "Mã chương trình không được để trống")
    @Size(max = 50, message = "Mã chương trình không được vượt quá 50 ký tự")
    private String code;

    @NotBlank(message = "Tên chương trình không được để trống")
    @Size(max = 255, message = "Tên chương trình không được vượt quá 255 ký tự")
    private String name;

    private String description;

    @NotNull(message = "Thời gian (giờ) không được để trống")
    @Positive(message = "Thời gian (giờ) phải là số dương")
    private Integer durationHours;

    @NotNull(message = "Hình thức học không được để trống")
    private DeliveryMode deliveryMode;

    @NotBlank(message = "Danh mục không được để trống")
    @Size(max = 50, message = "Danh mục không được vượt quá 50 ký tự")
    private String categoryCode;

    private String languageCode;

    private Boolean isActive = true;

    // Getters and Setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(Integer durationHours) {
        this.durationHours = durationHours;
    }

    public DeliveryMode getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(DeliveryMode deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}


package com.example.sis.dtos.program;

import com.example.sis.models.Program.DeliveryMode;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * DTO cho request cập nhật Program
 * Tất cả fields đều optional vì có thể chỉ cập nhật 1 vài trường
 */
public class UpdateProgramRequest {

    @Size(max = 255, message = "Tên chương trình không được vượt quá 255 ký tự")
    private String name;

    private String description;

    @Positive(message = "Thời gian (giờ) phải là số dương")
    private Integer durationHours;

    private DeliveryMode deliveryMode;

    @Size(max = 50, message = "Danh mục không được vượt quá 50 ký tự")
    private String categoryCode;

    private String languageCode;

    private Boolean isActive;

    // Getters and Setters
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


package com.example.sis.dtos.module;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO để sắp xếp lại thứ tự module trong program
 * 
 * Logic:
 * - sequence_order liên tục từ 1 đến n
 * - Mỗi semester chứa 6 module (semester = ceil(sequence_order / 6))
 * - Khi di chuyển module từ vị trí A sang vị trí B, các module khác tự động dịch chuyển
 */
public class ReorderModuleRequest {

    @NotNull(message = "Vị trí mới là bắt buộc")
    @Positive(message = "Vị trí mới phải là số dương")
    private Integer newSequenceOrder;

    // ===== Getters & Setters =====
    public Integer getNewSequenceOrder() {
        return newSequenceOrder;
    }

    public void setNewSequenceOrder(Integer newSequenceOrder) {
        this.newSequenceOrder = newSequenceOrder;
    }
}


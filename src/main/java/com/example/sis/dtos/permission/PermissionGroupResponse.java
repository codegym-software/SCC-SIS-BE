package com.example.sis.dtos.permission;

import java.util.List;

/**
 * DTO cho nhóm permission theo category
 */
public class PermissionGroupResponse {
    private String category;
    private String categoryLabel;
    private Integer order;
    private Integer total;
    private List<PermissionGroupItemResponse> items;

    // Constructors
    public PermissionGroupResponse() {
    }

    public PermissionGroupResponse(String category, String categoryLabel, Integer order,
                                   Integer total, List<PermissionGroupItemResponse> items) {
        this.category = category;
        this.categoryLabel = categoryLabel;
        this.order = order;
        this.total = total;
        this.items = items;
    }

    // Getters and Setters
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryLabel() {
        return categoryLabel;
    }

    public void setCategoryLabel(String categoryLabel) {
        this.categoryLabel = categoryLabel;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public List<PermissionGroupItemResponse> getItems() {
        return items;
    }

    public void setItems(List<PermissionGroupItemResponse> items) {
        this.items = items;
    }
}
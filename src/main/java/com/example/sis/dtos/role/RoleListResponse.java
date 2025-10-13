package com.example.sis.dtos.role;

import java.util.List;

public class RoleListResponse {
    private int total;
    private List<RoleListItem> items;

    public RoleListResponse() {}

    public RoleListResponse(int total, List<RoleListItem> items) {
        this.total = total;
        this.items = items;
    }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public List<RoleListItem> getItems() { return items; }
    public void setItems(List<RoleListItem> items) { this.items = items; }
}
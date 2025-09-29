package com.example.sis.dtos.user;

import java.util.ArrayList;
import java.util.List;

public class UserViewResponse {

    // --- User fields ---
    private Integer userId;
    private String fullName;
    private String email;
    private String phone;
    private boolean active;
    private String specialty; // chuyên môn (có thể null)

    // --- Assignments ---
    private List<AssignmentItemResponse> assignments = new ArrayList<>();

    public UserViewResponse() {
    }

    public UserViewResponse(Integer userId,
                            String fullName,
                            String email,
                            String phone,
                            boolean active,
                            String specialty,
                            List<AssignmentItemResponse> assignments) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.active = active;
        this.specialty = specialty;
        if (assignments != null) {
            this.assignments = assignments;
        }
    }

    // --- getters/setters ---

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public List<AssignmentItemResponse> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<AssignmentItemResponse> assignments) {
        this.assignments = assignments;
    }
}

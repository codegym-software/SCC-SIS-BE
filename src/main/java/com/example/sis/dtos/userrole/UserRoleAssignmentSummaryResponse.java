package com.example.sis.dtos.userrole;

import java.util.ArrayList;
import java.util.List;

public class UserRoleAssignmentSummaryResponse {

    private int createdCount;
    private int skippedCount;
    private List<String> errors;

    public UserRoleAssignmentSummaryResponse() {
        this.createdCount = 0;
        this.skippedCount = 0;
        this.errors = new ArrayList<>();
    }

    public UserRoleAssignmentSummaryResponse(int createdCount, int skippedCount, List<String> errors) {
        this.createdCount = createdCount;
        this.skippedCount = skippedCount;
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public int getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(int createdCount) {
        this.createdCount = createdCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public void incrementCreated() {
        this.createdCount++;
    }

    public void incrementSkipped() {
        this.skippedCount++;
    }

    public void addError(String error) {
        this.errors.add(error);
    }
}
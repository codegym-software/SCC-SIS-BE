package com.example.sis.dtos.classes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class CreateClassRequest {

    @NotNull(message = "ID chương trình học không được để trống")
    private Integer programId;

    @NotBlank(message = "Tên lớp không được để trống")
    @Size(max = 255, message = "Tên lớp không được vượt quá 255 ký tự")
    private String name;

    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    @Size(max = 100, message = "Phòng học không được vượt quá 100 ký tự")
    private String room;

    @Positive(message = "Sức chứa phải là số dương")
    private Integer capacity;

    // Constructors
    public CreateClassRequest() {
    }

    // Getters and Setters
    public Integer getProgramId() {
        return programId;
    }

    public void setProgramId(Integer programId) {
        this.programId = programId;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }
}
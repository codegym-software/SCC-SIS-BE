package com.example.sis.dtos.classes;

import com.example.sis.enums.StudyDay;
import com.example.sis.enums.StudyTime;
import com.example.sis.models.ClassEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class UpdateClassRequest {

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

    // Ngày học trong tuần (tối đa 2 ngày)
    private List<StudyDay> studyDays;

    // Ca học
    private StudyTime studyTime;

    // Trạng thái lớp học
    private ClassEntity.ClassStatus status;

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

    public List<StudyDay> getStudyDays() {
        return studyDays;
    }

    public void setStudyDays(List<StudyDay> studyDays) {
        this.studyDays = studyDays;
    }

    public StudyTime getStudyTime() {
        return studyTime;
    }

    public void setStudyTime(StudyTime studyTime) {
        this.studyTime = studyTime;
    }

    public ClassEntity.ClassStatus getStatus() {
        return status;
    }

    public void setStatus(ClassEntity.ClassStatus status) {
        this.status = status;
    }
}
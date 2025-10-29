package com.example.sis.models;

import com.example.sis.converters.StudyDaysConverter;
import com.example.sis.enums.StudyDay;
import com.example.sis.enums.StudyTime;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "classes", indexes = {
                @Index(name = "idx_classes_center_status_start_id", columnList = "center_id, status, start_date, class_id"),
                @Index(name = "idx_classes_program", columnList = "program_id")
}, uniqueConstraints = {
                @UniqueConstraint(name = "uk_classes_center_name", columnNames = { "center_id", "name" })
})
public class ClassEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "class_id")
        private Integer classId;

        @ManyToOne
        @JoinColumn(name = "center_id", nullable = false, foreignKey = @ForeignKey(name = "fk_classes_center"))
        private Center center;

        @ManyToOne
        @JoinColumn(name = "program_id", nullable = false, foreignKey = @ForeignKey(name = "fk_classes_program"))
        private Program program;

        @Column(name = "name", nullable = false, length = 255)
        private String name;

        @Column(name = "description", columnDefinition = "TEXT")
        private String description;

        @Column(name = "start_date")
        private LocalDate startDate;

        @Column(name = "end_date")
        private LocalDate endDate;

        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false, length = 20)
        private ClassStatus status = ClassStatus.PLANNED;

        @Column(name = "room", length = 100)
        private String room;

        @Column(name = "capacity")
        private Integer capacity;

        @Column(name = "study_days")
        @Convert(converter = StudyDaysConverter.class)
        private List<StudyDay> studyDays;

        @Enumerated(EnumType.STRING)
        @Column(name = "study_time", length = 20)
        private StudyTime studyTime;

        @Column(name = "deleted_at")
        private LocalDateTime deletedAt;

        @Column(name = "created_at", nullable = false, updatable = false)
        private LocalDateTime createdAt;

        @Column(name = "updated_at", nullable = false)
        private LocalDateTime updatedAt;

        @ManyToOne
        @JoinColumn(name = "created_by", foreignKey = @ForeignKey(name = "fk_classes_created_by"))
        private User createdBy;

        @ManyToOne
        @JoinColumn(name = "updated_by", foreignKey = @ForeignKey(name = "fk_classes_updated_by"))
        private User updatedBy;

        @OneToMany(mappedBy = "classEntity", fetch = FetchType.LAZY)
        private List<ClassTeacher> classTeachers = new ArrayList<>();

        public enum ClassStatus {
                PLANNED, ONGOING, FINISHED, CANCELLED
        }

        // ===== Getter & Setter =====
        public Integer getClassId() {
                return classId;
        }

        public void setClassId(Integer classId) {
                this.classId = classId;
        }

        public Center getCenter() {
                return center;
        }

        public void setCenter(Center center) {
                this.center = center;
        }

        public Program getProgram() {
                return program;
        }

        public void setProgram(Program program) {
                this.program = program;
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

        public ClassStatus getStatus() {
                return status;
        }

        public void setStatus(ClassStatus status) {
                this.status = status;
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

        public LocalDateTime getDeletedAt() {
                return deletedAt;
        }

        public void setDeletedAt(LocalDateTime deletedAt) {
                this.deletedAt = deletedAt;
        }

        public LocalDateTime getCreatedAt() {
                return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
                this.createdAt = createdAt;
        }

        public LocalDateTime getUpdatedAt() {
                return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
                this.updatedAt = updatedAt;
        }

        public User getCreatedBy() {
                return createdBy;
        }

        public void setCreatedBy(User createdBy) {
                this.createdBy = createdBy;
        }

        public User getUpdatedBy() {
                return updatedBy;
        }

        public void setUpdatedBy(User updatedBy) {
                this.updatedBy = updatedBy;
        }

        public List<ClassTeacher> getClassTeachers() {
                return classTeachers;
        }

        public void setClassTeachers(List<ClassTeacher> classTeachers) {
                this.classTeachers = classTeachers;
        }
}

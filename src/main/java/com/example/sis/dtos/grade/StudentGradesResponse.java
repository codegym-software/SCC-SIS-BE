package com.example.sis.dtos.grade;

import com.example.sis.dtos.module.ModuleResponse;

import java.util.List;

/**
 * Response cho API lấy điểm của học viên theo lớp, semester, module
 * Có 2 mode:
 * - Không có moduleId: trả về danh sách modules để chọn
 * - Có moduleId: trả về danh sách grade records của học viên
 */
public class StudentGradesResponse {

    private List<ModuleResponse> modules; // Danh sách modules (khi chưa chọn module)
    private List<GradeRecordResponse> gradeRecords; // Danh sách điểm (khi đã chọn module)
    private Integer classId;
    private String className;
    private Integer semester;
    private Integer moduleId; // Module đã chọn (nếu có)

    public List<ModuleResponse> getModules() {
        return modules;
    }

    public void setModules(List<ModuleResponse> modules) {
        this.modules = modules;
    }

    public List<GradeRecordResponse> getGradeRecords() {
        return gradeRecords;
    }

    public void setGradeRecords(List<GradeRecordResponse> gradeRecords) {
        this.gradeRecords = gradeRecords;
    }

    public Integer getClassId() {
        return classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public Integer getModuleId() {
        return moduleId;
    }

    public void setModuleId(Integer moduleId) {
        this.moduleId = moduleId;
    }
}


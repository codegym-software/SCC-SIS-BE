package com.example.sis.dtos.student;

import com.example.sis.enums.GenderType;
import com.example.sis.enums.OverallStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class StudentDTO {

    private Integer studentId;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 255, message = "Họ và tên không được vượt quá 255 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @PastOrPresent(message = "Ngày sinh phải là quá khứ hoặc hiện tại")
    private LocalDate dob;

    private GenderType gender;

    @Size(max = 64, message = "Số CMND/CCCD không được vượt quá 64 ký tự")
    private String nationalIdNo;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String addressLine;

    @Size(max = 128, message = "Tỉnh/Thành phố không được vượt quá 128 ký tự")
    private String province;

    @Size(max = 128, message = "Quận/Huyện không được vượt quá 128 ký tự")
    private String district;

    @Size(max = 128, message = "Phường/Xã không được vượt quá 128 ký tự")
    private String ward;

    @NotNull(message = "Trạng thái tổng quát không được để trống")
    private OverallStatus overallStatus;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String note;

    // Constructors
    public StudentDTO() {
    }

    // Getters and Setters
    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
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

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public GenderType getGender() {
        return gender;
    }

    public void setGender(GenderType gender) {
        this.gender = gender;
    }

    public String getNationalIdNo() {
        return nationalIdNo;
    }

    public void setNationalIdNo(String nationalIdNo) {
        this.nationalIdNo = nationalIdNo;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public OverallStatus getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(OverallStatus overallStatus) {
        this.overallStatus = overallStatus;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
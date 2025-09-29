package com.example.sis.dtos.center;

public class CenterLiteResponse {
    private Integer centerId;
    private String code;
    private String name;

    public CenterLiteResponse() {}

    public CenterLiteResponse(Integer centerId, String code, String name) {
        this.centerId = centerId;
        this.code = code;
        this.name = name;
    }

    public Integer getCenterId() { return centerId; }
    public void setCenterId(Integer centerId) { this.centerId = centerId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

package com.app.sis.dto;

public class AIChatResponseDto {
    private String message;
    private String responseType; // "rule-based" or "openai"
    private boolean success = true;

    public AIChatResponseDto() {
    }

    public AIChatResponseDto(String message, String responseType, boolean success) {
        this.message = message;
        this.responseType = responseType;
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}

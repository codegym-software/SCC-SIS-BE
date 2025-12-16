package com.app.sis.dto;

public class AIChatRequestDto {
    private String message;
    private Long userId;
    private String userName;
    private boolean useOpenAI = false; // Default to rule-based

    public AIChatRequestDto() {
    }

    public AIChatRequestDto(String message, Long userId, String userName, boolean useOpenAI) {
        this.message = message;
        this.userId = userId;
        this.userName = userName;
        this.useOpenAI = useOpenAI;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public boolean isUseOpenAI() {
        return useOpenAI;
    }

    public void setUseOpenAI(boolean useOpenAI) {
        this.useOpenAI = useOpenAI;
    }
}

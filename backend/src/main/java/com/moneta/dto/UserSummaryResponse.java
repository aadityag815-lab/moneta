package com.moneta.dto;

public class UserSummaryResponse {

    private Long userId;
    private String username;
    private String fullName;
    private String phoneNumber;

    public UserSummaryResponse() {
    }

    public UserSummaryResponse(Long userId, String username, String fullName, String phoneNumber) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}

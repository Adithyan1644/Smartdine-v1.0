package com.smartdine.dto;

public class LoginRequest {
    private String username;
    private String password;

    // Default Constructor
    public LoginRequest() {}

    // Getters & Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
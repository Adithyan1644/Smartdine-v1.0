package com.smartdine.dto;

import java.util.UUID;

public class AuthResponse {
    private String token;
    private String role;
    private UUID restaurantId;
    private String fullName;

    // Constructors
    public AuthResponse() {}

    public AuthResponse(String token, String role, UUID restaurantId, String fullName) {
        this.token = token;
        this.role = role;
        this.restaurantId = restaurantId;
        this.fullName = fullName;
    }

    // Getters & Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public UUID getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(UUID restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
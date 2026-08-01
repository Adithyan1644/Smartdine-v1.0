package com.smartdine.dto;

public class LoginRequest {
    private String username;
    private String restaurantName;
    private String phone;
    private String password;

    public LoginRequest() {}

    public String getUsername() {
        if (username != null && !username.trim().isEmpty()) return username;
        if (restaurantName != null && !restaurantName.trim().isEmpty()) return restaurantName;
        return phone;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
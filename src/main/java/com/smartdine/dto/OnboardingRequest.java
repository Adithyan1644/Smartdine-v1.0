package com.smartdine.dto;

import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class OnboardingRequest {

    @NotBlank(message = "Restaurant name is required")
    private String restaurantName;

    private String ownerName;

    private String phone;

    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @JsonProperty("isTest")
    private boolean isTest = false;

    // Operational setup data entered during onboarding/signup
    private List<String> areas;
    private List<Map<String, Object>> tables;
    private List<String> menuCategories;
    private List<Map<String, Object>> menuItems;

    public OnboardingRequest() {}

    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isTest() { return isTest; }
    public void setTest(boolean isTest) { this.isTest = isTest; }

    public List<String> getAreas() { return areas; }
    public void setAreas(List<String> areas) { this.areas = areas; }

    public List<Map<String, Object>> getTables() { return tables; }
    public void setTables(List<Map<String, Object>> tables) { this.tables = tables; }

    public List<String> getMenuCategories() { return menuCategories; }
    public void setMenuCategories(List<String> menuCategories) { this.menuCategories = menuCategories; }

    public List<Map<String, Object>> getMenuItems() { return menuItems; }
    public void setMenuItems(List<Map<String, Object>> menuItems) { this.menuItems = menuItems; }
}

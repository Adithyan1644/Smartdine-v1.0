package com.smartdine.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Type-safe DTO for new tenant registration.
 * Carries the isTest flag so the cloud can classify
 * this restaurant as a Live Production or Testing/Demo account.
 */
public class RegisterRequest {

    @NotBlank(message = "Restaurant name is required")
    private String restaurantName;

    @NotBlank(message = "Owner name is required")
    private String ownerName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    /**
     * false = Live Production account (default).
     * true  = Testing / Demo account (isolated from live reports).
     *
     * @JsonProperty REQUIRED — Java boolean naming convention causes Jackson
     * to map setTest() → JSON key "test", NOT "isTest".
     * This annotation forces the correct mapping: JSON "isTest" → this field.
     */
    @JsonProperty("isTest")
    private boolean isTest = false;

    public RegisterRequest() {}

    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isTest() { return isTest; }
    public void setTest(boolean isTest) { this.isTest = isTest; }
}

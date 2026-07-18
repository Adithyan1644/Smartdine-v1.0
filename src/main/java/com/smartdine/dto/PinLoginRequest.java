package com.smartdine.dto;

import java.util.UUID;

public class PinLoginRequest {
    private String pin;
    private UUID restaurantId; // Staff must be tied to a restaurant

    // Default Constructor
    public PinLoginRequest() {}

    // Getters & Setters
    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public UUID getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(UUID restaurantId) {
        this.restaurantId = restaurantId;
    }
}
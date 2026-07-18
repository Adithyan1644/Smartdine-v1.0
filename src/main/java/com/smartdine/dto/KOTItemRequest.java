package com.smartdine.dto;

import java.util.UUID;

public class KOTItemRequest {
    private UUID menuItemId;         // The ID of the food item (e.g. Paneer Tikka)
    private int quantity;            // How many portions (e.g. 2)
    private String specialInstruction; // e.g. "Less spicy", "No onion"
    private java.util.List<UUID> modifierOptionIds = new java.util.ArrayList<>();

    // Default Constructor
    public KOTItemRequest() {}

    // Getters & Setters
    public UUID getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(UUID menuItemId) {
        this.menuItemId = menuItemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getSpecialInstruction() {
        return specialInstruction;
    }

    public void setSpecialInstruction(String specialInstruction) {
        this.specialInstruction = specialInstruction;
    }

    public java.util.List<UUID> getModifierOptionIds() {
        return modifierOptionIds;
    }

    public void setModifierOptionIds(java.util.List<UUID> modifierOptionIds) {
        this.modifierOptionIds = modifierOptionIds;
    }

    @Override
    public String toString() {
        return "KOTItemRequest{" +
                "menuItemId=" + menuItemId +
                ", quantity=" + quantity +
                ", specialInstruction='" + specialInstruction + '\'' +
                ", modifierOptionIds=" + modifierOptionIds +
                '}';
    }
}
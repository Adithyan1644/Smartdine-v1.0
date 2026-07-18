package com.smartdine.coreheart;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "kot_items")
public class KOTItem extends BaseEntity {

    @Column(name = "menu_item_id", nullable = false)
    private UUID menuItemId;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "special_instruction")
    private String specialInstruction; // e.g., "Less spicy", "No onion"

    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", nullable = false)
    private KOTStatus itemStatus = KOTStatus.PENDING;

    // Constructors
    public KOTItem() {}

    public KOTItem(UUID menuItemId, String itemName, int quantity, String specialInstruction, KOTStatus itemStatus) {
        this.menuItemId = menuItemId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.specialInstruction = specialInstruction;
        this.itemStatus = itemStatus;
    }

    // Getters & Setters
    public UUID getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(UUID menuItemId) {
        this.menuItemId = menuItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
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

    public KOTStatus getItemStatus() {
        return itemStatus;
    }

    public void setItemStatus(KOTStatus itemStatus) {
        this.itemStatus = itemStatus;
    }
}
package com.smartdine.dto;

import java.util.List;
import java.util.UUID;

public class OrderRequest {
    private UUID tableId;               // Which table is ordering (e.g., Table 5)
    private List<KOTItemRequest> items; // The list of food items
    private String notes;               // Global order notes (e.g. "Anniversary couple")

    // Default Constructor
    public OrderRequest() {}

    // Getters & Setters
    public UUID getTableId() {
        return tableId;
    }

    public void setTableId(UUID tableId) {
        this.tableId = tableId;
    }

    public List<KOTItemRequest> getItems() {
        return items;
    }

    public void setItems(List<KOTItemRequest> items) {
        this.items = items;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "OrderRequest{" +
                "tableId=" + tableId +
                ", items=" + items +
                ", notes='" + notes + '\'' +
                '}';
    }
}
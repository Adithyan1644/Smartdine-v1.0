package com.smartdine.coreheart;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "kots")
public class KOT extends BaseEntity {

    @Column(name = "kot_number", nullable = false)
    private String kotNumber; // e.g., "KOT-1087"
    
    @Column(name = "order_id", nullable = false)
    private UUID orderId; 
    
    @Column(name = "table_id", nullable = false)
    private UUID tableId;
    
    @Column(name = "table_name")
    private String tableName;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "kot_id")
    private List<KOTItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_status", nullable = false)
    private KOTStatus overallStatus = KOTStatus.PENDING;

    @Column(name = "notes")
    private String notes;

    // Constructors
    public KOT() {}

    public KOT(String kotNumber, UUID orderId, UUID tableId, String tableName, List<KOTItem> items, KOTStatus overallStatus, String notes) {
        this.kotNumber = kotNumber;
        this.orderId = orderId;
        this.tableId = tableId;
        this.tableName = tableName;
        this.items = items;
        this.overallStatus = overallStatus;
        this.notes = notes;
    }

    // Getters & Setters
    public String getKotNumber() {
        return kotNumber;
    }

    public void setKotNumber(String kotNumber) {
        this.kotNumber = kotNumber;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getTableId() {
        return tableId;
    }

    public void setTableId(UUID tableId) {
        this.tableId = tableId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<KOTItem> getItems() {
        return items;
    }

    public void setItems(List<KOTItem> items) {
        this.items = items;
    }

    public KOTStatus getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(KOTStatus overallStatus) {
        this.overallStatus = overallStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
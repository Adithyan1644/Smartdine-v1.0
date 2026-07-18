package com.smartdine.coreheart;

import jakarta.persistence.*;

@Entity
@Table(name = "dining_tables")
public class DiningTable extends BaseEntity {

    @Column(name = "table_number", nullable = false)
    private String tableNumber; // e.g., "ACT1", "GT5"

    private int capacity; // e.g., 4 seater

    @Enumerated(EnumType.STRING)
    private TableStatus status = TableStatus.AVAILABLE;

    private String areaName; // e.g., "AC Area", "Garden"

    @Transient
    private double totalAmount;

    @Transient
    private int durationMinutes;

    @Transient
    private boolean merged;

    @Transient
    private String mergedTableNames;

    // Constructors
    public DiningTable() {}

    public DiningTable(String tableNumber, int capacity, TableStatus status, String areaName, double totalAmount, int durationMinutes) {
        this.tableNumber = tableNumber;
        this.capacity = capacity;
        this.status = status;
        this.areaName = areaName;
        this.totalAmount = totalAmount;
        this.durationMinutes = durationMinutes;
    }

    // Getters & Setters
    public String getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public TableStatus getStatus() {
        return status;
    }

    public void setStatus(TableStatus status) {
        this.status = status;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public boolean isMerged() {
        return merged;
    }

    public void setMerged(boolean merged) {
        this.merged = merged;
    }

    public String getMergedTableNames() {
        return mergedTableNames;
    }

    public void setMergedTableNames(String mergedTableNames) {
        this.mergedTableNames = mergedTableNames;
    }
}
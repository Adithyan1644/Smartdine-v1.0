package com.smartdine.coreheart;

import jakarta.persistence.*;

@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

    @Column(nullable = false)
    private String phone;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private int visitCount = 0;

    // Constructors
    public Customer() {}

    public Customer(String phone, String name, String notes) {
        this.phone = phone;
        this.name = name;
        this.notes = notes;
        this.visitCount = 1;
    }

    // Getters & Setters
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getVisitCount() {
        return visitCount;
    }

    public void setVisitCount(int visitCount) {
        this.visitCount = visitCount;
    }
}

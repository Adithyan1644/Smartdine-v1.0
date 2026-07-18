package com.smartdine.coreheart;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "app_users")
public class AppUser extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String username;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(length = 4)
    private String pin; // 4-digit PIN for staff login

    // Constructors
    public AppUser() {}

    public AppUser(String username, String password, UserRole role, String fullName, boolean isActive, String pin) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.fullName = fullName;
        this.isActive = isActive;
        this.pin = pin;
    }

    // Getters & Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}
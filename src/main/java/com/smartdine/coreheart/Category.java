package com.smartdine.coreheart;

import jakarta.persistence.*;

@Entity
@Table(name = "menu_categories")
public class Category extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String description;

    // Constructors
    public Category() {}

    // Getters & Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
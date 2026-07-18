package com.smartdine.coreheart;

import jakarta.persistence.*;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;

@Entity
@Table(name = "menu_items")
public class MenuItem extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(length = 10)
    private String shortCode; 

    @Column(name = "is_veg", nullable = false)
    @JsonProperty("isVeg") // ✅ FIX: Forces Jackson to use "isVeg" exactly for JSON mapping!
    private boolean isVeg = true; 

    private String categoryName; 

    private String description;

    @Column(nullable = false)
    private BigDecimal price; 

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "is_available")
    private boolean isAvailable = true;
    
    @Column(name = "is_todays_menu")
    private Boolean isTodaysMenu = true;
    
    private String imageUrl;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "modifier_group_id")
    private ModifierGroup modifierGroup;

    // Constructor
    public MenuItem() {}

    // Getters & Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public boolean isVeg() { return isVeg; }
    public void setVeg(boolean isVeg) { this.isVeg = isVeg; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Category getCategory() { return category; }
    @JsonProperty("category")
    public void setCategory(Category category) { this.category = category; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean isAvailable) { this.isAvailable = isAvailable; }

    public boolean isTodaysMenu() { return isTodaysMenu != null && isTodaysMenu; }
    public void setTodaysMenu(Boolean isTodaysMenu) { this.isTodaysMenu = isTodaysMenu; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public ModifierGroup getModifierGroup() { return modifierGroup; }
    public void setModifierGroup(ModifierGroup modifierGroup) { this.modifierGroup = modifierGroup; }
}
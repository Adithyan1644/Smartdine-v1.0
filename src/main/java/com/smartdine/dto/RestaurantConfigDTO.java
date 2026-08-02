package com.smartdine.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RestaurantConfigDTO implements Serializable {
    private UUID restaurantId;
    private String restaurantName;
    private boolean isTest;
    private List<Map<String, Object>> areas;
    private List<Map<String, Object>> tables;
    private List<Map<String, Object>> menuCategories;
    private List<String> categories;
    private List<Map<String, Object>> menuItems;
    private List<Map<String, Object>> modifierGroups;

    public RestaurantConfigDTO() {}

    public UUID getRestaurantId() { return restaurantId; }
    public void setRestaurantId(UUID restaurantId) { this.restaurantId = restaurantId; }

    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }

    public boolean isTest() { return isTest; }
    public void setTest(boolean isTest) { this.isTest = isTest; }

    public List<Map<String, Object>> getAreas() { return areas; }
    public void setAreas(List<Map<String, Object>> areas) { this.areas = areas; }

    public List<Map<String, Object>> getTables() { return tables; }
    public void setTables(List<Map<String, Object>> tables) { this.tables = tables; }

    public List<Map<String, Object>> getMenuCategories() { return menuCategories; }
    public void setMenuCategories(List<Map<String, Object>> menuCategories) { this.menuCategories = menuCategories; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    public List<Map<String, Object>> getMenuItems() { return menuItems; }
    public void setMenuItems(List<Map<String, Object>> menuItems) { this.menuItems = menuItems; }

    public List<Map<String, Object>> getModifierGroups() { return modifierGroups; }
    public void setModifierGroups(List<Map<String, Object>> modifierGroups) { this.modifierGroups = modifierGroups; }
}

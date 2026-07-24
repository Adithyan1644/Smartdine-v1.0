package com.smartdine.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.smartdine.coreheart.Category;
import com.smartdine.coreheart.MenuItem;
import com.smartdine.coreheart.TenantContext;
import com.smartdine.repository.CategoryRepository;
import com.smartdine.repository.MenuRepository;
import com.smartdine.service.MenuService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/menu")
public class MenuController {

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private MenuService menuService; // Inject the service

    // 1. Add a New Category (e.g., Starters)
    @PostMapping("/categories")
    public Category createCategory(@RequestBody Category category) {
        // Automatically assign the Restaurant ID from our TenantContext
        category.setRestaurantId(TenantContext.getRestaurantId());
        return categoryRepository.save(category);
    }

    // 1b. Get Categories for the Waiter App / Admin
    @GetMapping("/categories")
    public List<Category> getCategories() {
        UUID restaurantId = TenantContext.getRestaurantId();
        List<Category> rawCategories = categoryRepository.findByRestaurantId(restaurantId);
        if (rawCategories == null) {
            return java.util.Collections.emptyList();
        }
        
        // Deduplicate categories by normalized category name and exclude deleted categories
        java.util.Map<String, Category> uniqueMap = new java.util.LinkedHashMap<>();
        for (Category c : rawCategories) {
            if (c.isDeleted()) continue;
            String key = c.getName() != null ? c.getName().trim().toLowerCase() : c.getId().toString();
            if (!uniqueMap.containsKey(key)) {
                uniqueMap.put(key, c);
            }
        }
        return new java.util.ArrayList<>(uniqueMap.values());
    }

    // 2. Add a New Food Item
    @PostMapping("/items")
    public MenuItem createMenuItem(@RequestBody MenuItem item) {
        item.setRestaurantId(TenantContext.getRestaurantId());
        return menuService.saveItem(item);
    }

    // 3. Get Full Menu for the Waiter App
    @GetMapping("/items")
    public List<MenuItem> getMenu() {
        UUID restaurantId = TenantContext.getRestaurantId();
        List<MenuItem> rawItems = menuRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId);
        if (rawItems == null) {
            return java.util.Collections.emptyList();
        }

        // Deduplicate menu items by normalized item name / shortCode to prevent UI duplication
        java.util.Map<String, MenuItem> uniqueMap = new java.util.LinkedHashMap<>();
        for (MenuItem item : rawItems) {
            if (item.isDeleted()) continue;
            String key = (item.getName() != null ? item.getName().trim().toLowerCase() : "") + 
                         "_" + (item.getShortCode() != null ? item.getShortCode().trim().toUpperCase() : item.getId().toString());
            if (!uniqueMap.containsKey(key)) {
                uniqueMap.put(key, item);
            }
        }
        return new java.util.ArrayList<>(uniqueMap.values());
    }

    // 4. Toggle Availability (Stock Status)
    @PutMapping("/items/{id}/availability")
    public MenuItem toggleAvailability(@PathVariable UUID id, @RequestParam boolean available) {
        return menuService.toggleAvailability(id, available);
    }

    // 5. Toggle Today's Menu Status
    @PutMapping("/items/{id}/todays-menu")
    public MenuItem toggleTodaysMenu(@PathVariable UUID id, @RequestParam boolean active) {
        return menuService.toggleTodaysMenu(id, active);
    }
}
package com.smartdine.controller;

import com.smartdine.coreheart.Restaurant;
import com.smartdine.coreheart.DiningTable;
import com.smartdine.coreheart.Category;
import com.smartdine.repository.RestaurantRepository;
import com.smartdine.repository.TableRepository;
import com.smartdine.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/public/provision")
public class ProvisioningController {

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping("/activate")
    public ResponseEntity<?> activate(@RequestParam String code) {
        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sync code is required"));
        }
        String cleanedCode = code.trim().toLowerCase();
        java.io.File file = new java.io.File("activation-" + cleanedCode + ".json");
        if (file.exists()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map config = mapper.readValue(file, Map.class);
                return ResponseEntity.ok(config);
            } catch (Exception e) {
                System.err.println("⚠️ [ProvisioningController] Failed to read dynamic config: " + e.getMessage());
            }
        }

        Optional<Restaurant> restaurantOpt = restaurantRepository.findBySyncCodeAndIsDeletedFalse(code.trim());
        if (!restaurantOpt.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid Sync Code: " + code));
        }

        Restaurant restaurant = restaurantOpt.get();
        UUID restaurantId = restaurant.getRestaurantId();

        List<DiningTable> tables = tableRepository.findByRestaurantId(restaurantId);
        List<Category> categories = categoryRepository.findByRestaurantId(restaurantId);

        List<Map<String, Object>> mappedTables = new ArrayList<>();
        for (DiningTable t : tables) {
            mappedTables.add(Map.of(
                "tableNumber", t.getTableNumber(),
                "capacity", t.getCapacity(),
                "areaName", t.getAreaName()
            ));
        }

        List<String> mappedCategories = new ArrayList<>();
        for (Category c : categories) {
            mappedCategories.add(c.getName());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("restaurantId", restaurantId.toString());
        payload.put("restaurantName", restaurant.getName());
        payload.put("cgstRate", new java.math.BigDecimal("2.50"));
        payload.put("sgstRate", new java.math.BigDecimal("2.50"));
        payload.put("serviceChargeRate", new java.math.BigDecimal("0.00"));
        payload.put("tables", mappedTables);
        payload.put("categories", mappedCategories);
        payload.put("menuItems", Collections.emptyList());
        payload.put("modifierGroups", Collections.emptyList());
        
        List<Map<String, String>> defaultWaiters = List.of(
            Map.of("name", "Arjun Mehta", "pin", "4022", "status", "Active"),
            Map.of("name", "Priya Sharma", "pin", "4023", "status", "Active"),
            Map.of("name", "Rahul Verma", "pin", "4024", "status", "Active")
        );
        payload.put("waiters", defaultWaiters);

        return ResponseEntity.ok(payload);
    }
}

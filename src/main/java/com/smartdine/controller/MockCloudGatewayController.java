package com.smartdine.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/mock-cloud")
public class MockCloudGatewayController {

    @GetMapping("/activate")
    public ResponseEntity<?> getActivationConfig(@RequestParam String code) {
        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sync code is required"));
        }

        // Standardize code: SD-a0eebc99
        String cleanedCode = code.trim();
        if (!cleanedCode.equalsIgnoreCase("SD-a0eebc99") && !cleanedCode.equalsIgnoreCase("a0eebc99")) {
            try {
                String nodeUrl = "http://localhost:5000/api/activation/activate?code=" + cleanedCode;
                org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                Map config = restTemplate.getForObject(nodeUrl, Map.class);
                return ResponseEntity.ok(config);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid activation sync code or Node server is offline: " + e.getMessage()));
            }
        }

        // Mock cloud configuration response
        Map<String, Object> response = new HashMap<>();
        response.put("restaurantId", "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
        response.put("restaurantName", "SmartDine Elite Restaurant");
        response.put("cgstRate", new BigDecimal("2.50"));
        response.put("sgstRate", new BigDecimal("2.50"));
        response.put("serviceChargeRate", new BigDecimal("5.00"));

        // Categories
        response.put("categories", List.of("Starters", "Main Course", "Desserts", "Beverages"));

        // Tables
        List<Map<String, Object>> tables = List.of(
            Map.of("tableNumber", "T-01", "capacity", 4, "areaName", "AC Area"),
            Map.of("tableNumber", "T-02", "capacity", 2, "areaName", "AC Area"),
            Map.of("tableNumber", "T-03", "capacity", 4, "areaName", "AC Area"),
            Map.of("tableNumber", "T-04", "capacity", 6, "areaName", "AC Area"),
            Map.of("tableNumber", "T-05", "capacity", 2, "areaName", "Garden"),
            Map.of("tableNumber", "T-06", "capacity", 4, "areaName", "Garden"),
            Map.of("tableNumber", "T-07", "capacity", 4, "areaName", "Garden"),
            Map.of("tableNumber", "T-08", "capacity", 8, "areaName", "Garden")
        );
        response.put("tables", tables);

        // Menu Items
        List<Map<String, Object>> menuItems = List.of(
            Map.of("name", "Crispy Corn", "shortCode", "CC", "price", 180.00, "veg", true, "categoryName", "Starters"),
            Map.of("name", "Butter Chicken", "shortCode", "BC", "price", 380.00, "veg", false, "categoryName", "Main Course"),
            Map.of("name", "Paneer Butter Masala", "shortCode", "PBM", "price", 260.00, "veg", true, "categoryName", "Main Course"),
            Map.of("name", "Chocolate Brownie", "shortCode", "CB", "price", 150.00, "veg", true, "categoryName", "Desserts"),
            Map.of("name", "Virgin Mojito", "shortCode", "VM", "price", 120.00, "veg", true, "categoryName", "Beverages")
        );
        response.put("menuItems", menuItems);

        // Modifier Groups
        List<Map<String, Object>> modifierGroups = List.of(
            Map.of(
                "name", "Al-Faham Sides",
                "isGlobal", false,
                "options", List.of(
                    Map.of("name", "Khaboos", "price", 20.00),
                    Map.of("name", "Extra Mayonnaise", "price", 10.00)
                )
            ),
            Map.of(
                "name", "Global Drinks",
                "isGlobal", true,
                "options", List.of(
                    Map.of("name", "Water 500ml", "price", 10.00),
                    Map.of("name", "Water 1L", "price", 20.00),
                    Map.of("name", "Cold Drink", "price", 20.00)
                )
            ),
            Map.of(
                "name", "Global Sides & Sauces",
                "isGlobal", true,
                "options", List.of(
                    Map.of("name", "Mayonnaise", "price", 15.00),
                    Map.of("name", "Tomato Ketchup", "price", 0.00)
                )
            )
        );
        response.put("modifierGroups", modifierGroups);

        // Waiters
        List<Map<String, Object>> waiters = List.of(
            Map.of("name", "Ravi Kumar", "pin", "1001", "status", "Active"),
            Map.of("name", "Suresh Babu", "pin", "1002", "status", "Active"),
            Map.of("name", "Karthik M", "pin", "1003", "status", "Active"),
            Map.of("name", "Pradeep S", "pin", "1004", "status", "Disabled")
        );
        response.put("waiters", waiters);

        return ResponseEntity.ok(response);
    }
}

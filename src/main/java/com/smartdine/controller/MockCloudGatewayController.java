package com.smartdine.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * SmartDine Cloud Gateway — handles activation code handshake.
 * Each activation code maps to a specific restaurant's configuration.
 * The local Spring Boot instance calls this endpoint during the setup wizard
 * to pull the restaurant's tables, menu, staff and tax config.
 */
@RestController
@RequestMapping("/api/mock-cloud")
public class MockCloudGatewayController {

    // ─── Registry of all valid activation codes ───────────────────────────────
    // Key  = activation code (case-insensitive)
    // Value = restaurantId UUID
    private static final Map<String, String> CODE_TO_RESTAURANT = Map.of(
        "sd-28e792",    "28e79200-0000-4000-a000-000000000001",   // Your actual restaurant
        "sd-a0eebc99",  "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",  // Legacy demo restaurant
        "a0eebc99",     "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"   // Legacy shorthand
    );

    @GetMapping("/activate")
    public ResponseEntity<?> getActivationConfig(@RequestParam String code) {
        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sync code is required"));
        }

        String cleanedCode = code.trim().toLowerCase();

        // 1. Load dynamic activation-[code].json file if present
        java.io.File file = new java.io.File("activation-" + cleanedCode + ".json");
        if (!file.exists() && cleanedCode.equals("sd-28e792")) {
            file = new java.io.File("activation-data.json");
        }
        if (file.exists()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map config = mapper.readValue(file, Map.class);
                return ResponseEntity.ok(config);
            } catch (Exception e) {
                System.err.println("⚠️ [MockCloudGatewayController] Failed to read " + file.getName() + ": " + e.getMessage());
            }
        }

        // Look up the code in the registry
        if (!CODE_TO_RESTAURANT.containsKey(cleanedCode)) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid activation code: '" + code.trim() + "'. " +
                         "Please check your SmartDine subscription code."
            ));
        }

        String restaurantId = CODE_TO_RESTAURANT.get(cleanedCode);


        // Route to the appropriate restaurant config builder
        Map<String, Object> response = buildRestaurantConfig(cleanedCode, restaurantId);
        return ResponseEntity.ok(response);
    }

    // ─── Route to the correct config by code ─────────────────────────────────

    private Map<String, Object> buildRestaurantConfig(String code, String restaurantId) {
        if (code.equals("sd-28e792")) {
            return buildSD28E792Config(restaurantId);
        } else {
            // Legacy demo restaurant config (SD-a0eebc99)
            return buildDemoConfig(restaurantId);
        }
    }

    // ─── SD-28E792 — Your Restaurant Configuration ────────────────────────────

    private Map<String, Object> buildSD28E792Config(String restaurantId) {
        Map<String, Object> response = new HashMap<>();
        response.put("restaurantId",        restaurantId);
        response.put("restaurantName",      "SmartDine Restaurant");
        response.put("cgstRate",            new BigDecimal("2.50"));
        response.put("sgstRate",            new BigDecimal("2.50"));
        response.put("serviceChargeRate",   new BigDecimal("5.00"));

        // ── Categories ───────────────────────────────────────────────────────
        response.put("categories", List.of(
            "Starters", "Main Course", "Breads", "Rice & Biryani",
            "Desserts", "Beverages"
        ));

        // ── Tables ───────────────────────────────────────────────────────────
        List<Map<String, Object>> tables = List.of(
            Map.of("tableNumber", "T-01", "capacity", 2,  "areaName", "AC Area"),
            Map.of("tableNumber", "T-02", "capacity", 4,  "areaName", "AC Area"),
            Map.of("tableNumber", "T-03", "capacity", 4,  "areaName", "AC Area"),
            Map.of("tableNumber", "T-04", "capacity", 6,  "areaName", "AC Area"),
            Map.of("tableNumber", "T-05", "capacity", 4,  "areaName", "AC Area"),
            Map.of("tableNumber", "T-06", "capacity", 2,  "areaName", "Garden"),
            Map.of("tableNumber", "T-07", "capacity", 4,  "areaName", "Garden"),
            Map.of("tableNumber", "T-08", "capacity", 4,  "areaName", "Garden"),
            Map.of("tableNumber", "T-09", "capacity", 6,  "areaName", "Garden"),
            Map.of("tableNumber", "T-10", "capacity", 8,  "areaName", "Garden")
        );
        response.put("tables", tables);

        // ── Menu Items ───────────────────────────────────────────────────────
        List<Map<String, Object>> menuItems = List.of(
            // Starters
            Map.of("name", "Crispy Corn",           "shortCode", "CC",  "price", 180.00, "veg", true,  "categoryName", "Starters"),
            Map.of("name", "Paneer Tikka",           "shortCode", "PT",  "price", 320.00, "veg", true,  "categoryName", "Starters"),
            Map.of("name", "Chicken 65",             "shortCode", "C65", "price", 280.00, "veg", false, "categoryName", "Starters"),
            Map.of("name", "Veg Spring Roll",        "shortCode", "VSR", "price", 160.00, "veg", true,  "categoryName", "Starters"),
            // Main Course
            Map.of("name", "Butter Chicken",         "shortCode", "BC",  "price", 380.00, "veg", false, "categoryName", "Main Course"),
            Map.of("name", "Paneer Butter Masala",   "shortCode", "PBM", "price", 260.00, "veg", true,  "categoryName", "Main Course"),
            Map.of("name", "Dal Makhani",            "shortCode", "DM",  "price", 200.00, "veg", true,  "categoryName", "Main Course"),
            Map.of("name", "Chicken Curry",          "shortCode", "CHC", "price", 340.00, "veg", false, "categoryName", "Main Course"),
            // Breads
            Map.of("name", "Butter Naan",            "shortCode", "BN",  "price",  60.00, "veg", true,  "categoryName", "Breads"),
            Map.of("name", "Garlic Naan",            "shortCode", "GN",  "price",  70.00, "veg", true,  "categoryName", "Breads"),
            Map.of("name", "Tandoori Roti",          "shortCode", "TR",  "price",  30.00, "veg", true,  "categoryName", "Breads"),
            // Rice & Biryani
            Map.of("name", "Veg Biryani",            "shortCode", "VB",  "price", 220.00, "veg", true,  "categoryName", "Rice & Biryani"),
            Map.of("name", "Chicken Biryani",        "shortCode", "CB2", "price", 320.00, "veg", false, "categoryName", "Rice & Biryani"),
            Map.of("name", "Jeera Rice",             "shortCode", "JR",  "price", 120.00, "veg", true,  "categoryName", "Rice & Biryani"),
            // Desserts
            Map.of("name", "Chocolate Brownie",      "shortCode", "CB",  "price", 150.00, "veg", true,  "categoryName", "Desserts"),
            Map.of("name", "Gulab Jamun",            "shortCode", "GJ",  "price",  80.00, "veg", true,  "categoryName", "Desserts"),
            // Beverages
            Map.of("name", "Virgin Mojito",          "shortCode", "VM",  "price", 120.00, "veg", true,  "categoryName", "Beverages"),
            Map.of("name", "Mango Lassi",            "shortCode", "ML",  "price", 100.00, "veg", true,  "categoryName", "Beverages"),
            Map.of("name", "Cold Coffee",            "shortCode", "CCF", "price", 130.00, "veg", true,  "categoryName", "Beverages")
        );
        response.put("menuItems", menuItems);

        // ── Modifier Groups ──────────────────────────────────────────────────
        List<Map<String, Object>> modifierGroups = List.of(
            Map.of(
                "name", "Global Drinks",
                "isGlobal", true,
                "options", List.of(
                    Map.of("name", "Water 500ml",  "price", 10.00),
                    Map.of("name", "Water 1L",     "price", 20.00),
                    Map.of("name", "Cold Drink",   "price", 20.00)
                )
            ),
            Map.of(
                "name", "Global Sides & Sauces",
                "isGlobal", true,
                "options", List.of(
                    Map.of("name", "Mayonnaise",      "price", 15.00),
                    Map.of("name", "Tomato Ketchup",  "price",  0.00),
                    Map.of("name", "Green Chutney",   "price",  0.00)
                )
            ),
            Map.of(
                "name", "Naan Extras",
                "isGlobal", false,
                "options", List.of(
                    Map.of("name", "Extra Butter",   "price", 10.00),
                    Map.of("name", "Cheese Filling", "price", 30.00)
                )
            )
        );
        response.put("modifierGroups", modifierGroups);

        // ── Waiters ──────────────────────────────────────────────────────────
        List<Map<String, Object>> waiters = List.of(
            Map.of("name", "Arjun Mehta",  "pin", "4022", "status", "Active"),
            Map.of("name", "Priya Sharma", "pin", "4023", "status", "Active"),
            Map.of("name", "Rahul Verma",  "pin", "4024", "status", "Active")
        );
        response.put("waiters", waiters);

        return response;
    }

    // ─── Legacy Demo Config (SD-a0eebc99) ────────────────────────────────────

    private Map<String, Object> buildDemoConfig(String restaurantId) {
        Map<String, Object> response = new HashMap<>();
        response.put("restaurantId",        restaurantId);
        response.put("restaurantName",      "SmartDine Elite Restaurant");
        response.put("cgstRate",            new BigDecimal("2.50"));
        response.put("sgstRate",            new BigDecimal("2.50"));
        response.put("serviceChargeRate",   new BigDecimal("5.00"));

        response.put("categories", List.of("Starters", "Main Course", "Desserts", "Beverages"));

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

        List<Map<String, Object>> menuItems = List.of(
            Map.of("name", "Crispy Corn",           "shortCode", "CC",  "price", 180.00, "veg", true,  "categoryName", "Starters"),
            Map.of("name", "Butter Chicken",         "shortCode", "BC",  "price", 380.00, "veg", false, "categoryName", "Main Course"),
            Map.of("name", "Paneer Butter Masala",   "shortCode", "PBM", "price", 260.00, "veg", true,  "categoryName", "Main Course"),
            Map.of("name", "Chocolate Brownie",      "shortCode", "CB",  "price", 150.00, "veg", true,  "categoryName", "Desserts"),
            Map.of("name", "Virgin Mojito",          "shortCode", "VM",  "price", 120.00, "veg", true,  "categoryName", "Beverages")
        );
        response.put("menuItems", menuItems);

        List<Map<String, Object>> modifierGroups = List.of(
            Map.of("name", "Al-Faham Sides", "isGlobal", false,
                "options", List.of(
                    Map.of("name", "Khaboos",          "price", 20.00),
                    Map.of("name", "Extra Mayonnaise",  "price", 10.00)
                )
            ),
            Map.of("name", "Global Drinks", "isGlobal", true,
                "options", List.of(
                    Map.of("name", "Water 500ml", "price", 10.00),
                    Map.of("name", "Water 1L",    "price", 20.00),
                    Map.of("name", "Cold Drink",  "price", 20.00)
                )
            ),
            Map.of("name", "Global Sides & Sauces", "isGlobal", true,
                "options", List.of(
                    Map.of("name", "Mayonnaise",     "price", 15.00),
                    Map.of("name", "Tomato Ketchup", "price",  0.00)
                )
            )
        );
        response.put("modifierGroups", modifierGroups);

        List<Map<String, Object>> waiters = List.of(
            Map.of("name", "Ravi Kumar",  "pin", "1001", "status", "Active"),
            Map.of("name", "Suresh Babu", "pin", "1002", "status", "Active"),
            Map.of("name", "Karthik M",   "pin", "1003", "status", "Active"),
            Map.of("name", "Pradeep S",   "pin", "1004", "status", "Disabled")
        );
        response.put("waiters", waiters);

        return response;
    }
}

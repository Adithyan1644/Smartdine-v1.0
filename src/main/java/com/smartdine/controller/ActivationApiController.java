package com.smartdine.controller;

import com.smartdine.service.ActivationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/activation")
public class ActivationApiController {

    @Autowired
    private ActivationService activationService;

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        boolean activated = activationService.isSystemActivated();
        return ResponseEntity.ok(Map.of(
            "activated", activated,
            "restaurantId", activationService.getSystemConfig().map(c -> c.getRestaurantId().toString()).orElse(""),
            "restaurantName", activationService.getSystemConfig().map(c -> c.getRestaurantName()).orElse("")
        ));
    }

    @PostMapping("/activate")
    public ResponseEntity<?> activate(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String gateway = request.get("gatewayUrl");
        if (gateway == null || gateway.isEmpty()) {
            gateway = "http://localhost:8080/api/mock-cloud";
        }
        try {
            activationService.activateSystem(code, gateway);
            return ResponseEntity.ok(Map.of("success", true, "message", "System activated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/setup-manager")
    public ResponseEntity<?> setupManager(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String pin = request.get("pin");
        try {
            activationService.setupManagerAccount(username, password, pin);
            return ResponseEntity.ok(Map.of("success", true, "message", "Manager credentials configured successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/save-config")
    public ResponseEntity<?> saveConfig(@RequestBody Map<String, Object> request) {
        try {
            java.io.File file = new java.io.File("activation-data.json");
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            
            java.util.List<Map<String, Object>> incomingTables = (java.util.List<Map<String, Object>>) request.get("tables");
            java.util.List<Map<String, Object>> incomingMenuItems = (java.util.List<Map<String, Object>>) request.get("menuItems");
            java.util.List<Map<String, Object>> incomingWaiters = (java.util.List<Map<String, Object>>) request.get("waiters");
            
            java.util.Set<String> categories = new java.util.HashSet<>();
            if (incomingMenuItems != null) {
                for (Map<String, Object> item : incomingMenuItems) {
                    if (item.get("category") != null) {
                        categories.add(item.get("category").toString());
                    }
                }
            }
            
            java.util.List<Map<String, Object>> mappedTables = new java.util.ArrayList<>();
            if (incomingTables != null) {
                for (Map<String, Object> t : incomingTables) {
                    String number = t.get("number") != null ? t.get("number").toString() : "";
                    String prefix = number.startsWith("T-") ? "" : "T-";
                    String tableNum = prefix + number;
                    
                    mappedTables.add(Map.of(
                        "tableNumber", tableNum,
                        "capacity", t.get("capacity") != null ? Integer.parseInt(t.get("capacity").toString()) : 4,
                        "areaName", t.get("area") != null ? t.get("area").toString() : "General"
                    ));
                }
            }
            
            java.util.List<Map<String, Object>> mappedMenuItems = new java.util.ArrayList<>();
            if (incomingMenuItems != null) {
                for (Map<String, Object> item : incomingMenuItems) {
                    String name = item.get("name") != null ? item.get("name").toString() : "";
                    String shortCode = item.get("code") != null ? item.get("code").toString() : (name.length() >= 3 ? name.substring(0, 3).toUpperCase() : name.toUpperCase());
                    double price = item.get("price") != null ? Double.parseDouble(item.get("price").toString()) : 0.0;
                    boolean veg = item.get("type") != null ? item.get("type").toString().equalsIgnoreCase("Veg") : true;
                    String category = item.get("category") != null ? item.get("category").toString() : "General";
                    
                    mappedMenuItems.add(Map.of(
                        "name", name,
                        "shortCode", shortCode,
                        "price", price,
                        "veg", veg,
                        "categoryName", category
                    ));
                }
            }
            
            java.util.List<Map<String, Object>> mappedWaiters = new java.util.ArrayList<>();
            if (incomingWaiters != null) {
                for (Map<String, Object> w : incomingWaiters) {
                    mappedWaiters.add(Map.of(
                        "name", w.get("name") != null ? w.get("name").toString() : "",
                        "pin", w.get("pin") != null ? w.get("pin").toString() : (w.get("code") != null ? w.get("code").toString() : ""),
                        "status", w.get("status") != null ? w.get("status").toString() : "Active"
                    ));
                }
            }
            
            java.util.List<Map<String, Object>> modifierGroups = java.util.List.of(
                Map.of(
                    "name", "Global Drinks",
                    "isGlobal", true,
                    "options", java.util.List.of(
                        Map.of("name", "Water 500ml", "price", 10.0),
                        Map.of("name", "Water 1L", "price", 20.0),
                        Map.of("name", "Cold Drink", "price", 20.0)
                    )
                ),
                Map.of(
                    "name", "Global Sides & Sauces",
                    "isGlobal", true,
                    "options", java.util.List.of(
                        Map.of("name", "Mayonnaise", "price", 15.0),
                        Map.of("name", "Tomato Ketchup", "price", 0.0)
                    )
                )
            );
            
            Map<String, Object> gatewayPayload = new java.util.HashMap<>();
            gatewayPayload.put("restaurantId", "28e79200-0000-4000-a000-000000000001");
            gatewayPayload.put("restaurantName", request.get("restaurantName") != null ? request.get("restaurantName").toString() : "SmartDine Custom POS");
            gatewayPayload.put("cgstRate", new java.math.BigDecimal("2.50"));
            gatewayPayload.put("sgstRate", new java.math.BigDecimal("2.50"));
            gatewayPayload.put("serviceChargeRate", new java.math.BigDecimal("5.00"));
            gatewayPayload.put("categories", new java.util.ArrayList<>(categories));
            gatewayPayload.put("tables", mappedTables);
            gatewayPayload.put("menuItems", mappedMenuItems);
            gatewayPayload.put("modifierGroups", modifierGroups);
            gatewayPayload.put("waiters", mappedWaiters);
            
            mapper.writeValue(file, gatewayPayload);
            
            return ResponseEntity.ok(Map.of("success", true, "code", "SD-28E792"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}

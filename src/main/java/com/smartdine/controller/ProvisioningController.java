package com.smartdine.controller;

import com.smartdine.coreheart.Restaurant;
import com.smartdine.coreheart.DiningTable;
import com.smartdine.coreheart.Category;
import com.smartdine.coreheart.AppUser;
import com.smartdine.coreheart.UserRole;
import com.smartdine.repository.RestaurantRepository;
import com.smartdine.repository.TableRepository;
import com.smartdine.repository.CategoryRepository;
import com.smartdine.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Public provisioning endpoint used by the Waiter App to:
 *  1. Verify a restaurant's sync code (SD-XXXXXX)
 *  2. Resolve the restaurant UUID
 *  3. Receive the full tenant config (tables, categories, real waiters from DB)
 *
 * This endpoint is intentionally public (no JWT required) so the app
 * can activate before any waiter has logged in.
 */
@RestController
@RequestMapping("/api/public/provision")
@CrossOrigin(origins = "*")
public class ProvisioningController {

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/activate")
    public ResponseEntity<?> activate(@RequestParam String code) {
        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sync code is required"));
        }

        // ── Step 1: Try loading a cached wizard config file (Biller side) ──
        String cleanedCode = code.trim().toLowerCase();
        java.io.File file = new java.io.File("activation-" + cleanedCode + ".json");
        if (!file.exists()) {
            file = new java.io.File("core-heart/activation-" + cleanedCode + ".json");
        }
        if (!file.exists()) {
            file = new java.io.File("core-heart/core-heart/activation-" + cleanedCode + ".json");
        }
        if (file.exists()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> config = mapper.readValue(file, Map.class);

                // Even when reading from file, enrich with live waiters from DB
                // so that waiters added from the admin website are always visible
                Object restIdObj = config.get("restaurantId");
                if (restIdObj != null) {
                    try {
                        UUID restaurantId = UUID.fromString(restIdObj.toString());
                        List<Map<String, String>> liveWaiters = getLiveWaiters(restaurantId);
                        if (!liveWaiters.isEmpty()) {
                            config.put("waiters", liveWaiters);
                        }
                    } catch (IllegalArgumentException ignored) {
                        // If restaurantId is malformed, just return the file config as-is
                    }
                }
                return ResponseEntity.ok(config);
            } catch (Exception e) {
                System.err.println("⚠️ [ProvisioningController] Failed to read dynamic config: " + e.getMessage());
            }
        }

        // ── Step 2: Look up restaurant by sync code in DB ──
        Optional<Restaurant> restaurantOpt = restaurantRepository.findBySyncCodeAndIsDeletedFalse(code.trim());
        if (!restaurantOpt.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid Sync Code: " + code));
        }

        Restaurant restaurant = restaurantOpt.get();
        UUID restaurantId = restaurant.getRestaurantId();

        // ── Step 3: Load real tables and categories from DB ──
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

        // ── Step 4: Load REAL waiters from DB (not hardcoded dummies) ──
        List<Map<String, String>> realWaiters = getLiveWaiters(restaurantId);

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
        payload.put("waiters", realWaiters);

        return ResponseEntity.ok(payload);
    }

    /**
     * Fetches all active WAITER accounts for the given restaurant from the database.
     * This ensures the waiter app always shows real accounts added via the admin website.
     */
    private List<Map<String, String>> getLiveWaiters(UUID restaurantId) {
        List<AppUser> dbWaiters = userRepository.findByRestaurantIdAndRoleAndIsActiveTrue(
            restaurantId, UserRole.WAITER
        );
        List<Map<String, String>> result = new ArrayList<>();
        for (AppUser w : dbWaiters) {
            Map<String, String> entry = new HashMap<>();
            entry.put("name", w.getFullName() != null ? w.getFullName() : w.getUsername());
            entry.put("pin", w.getPin() != null ? w.getPin() : "");
            entry.put("status", "Active");
            result.add(entry);
        }
        return result;
    }

    // 1. Endpoint called by local Billing PC to report its IP on startup
    @PostMapping("/report-ip")
    public ResponseEntity<String> reportLocalIp(
            @RequestHeader("X-Restaurant-ID") UUID restaurantId,
            @RequestParam("ip") String ipAddress) {
        
        Restaurant restaurant = restaurantRepository.findByRestaurantId(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        
        restaurant.setActiveLocalIp(ipAddress);
        restaurantRepository.save(restaurant);
        
        System.out.println("☁️ GCP Cloud: Registered IP [" + ipAddress + "] for Restaurant: " + restaurant.getName());
        return ResponseEntity.ok("IP Registered");
    }

    // 2. Endpoint called by Waiter & KDS apps to fetch their local PC's live IP on boot
    @GetMapping("/active-ip")
    public ResponseEntity<Map<String, String>> getActiveIp(@RequestParam("syncCode") String syncCode) {
        Restaurant restaurant = restaurantRepository.findBySyncCodeAndIsDeletedFalse(syncCode.trim())
                .orElseThrow(() -> new RuntimeException("Invalid Sync Code: " + syncCode));

        return ResponseEntity.ok(Map.of(
            "restaurantId", restaurant.getRestaurantId().toString(),
            "localIp", restaurant.getActiveLocalIp() != null ? restaurant.getActiveLocalIp() : "127.0.0.1"
        ));
    }
}

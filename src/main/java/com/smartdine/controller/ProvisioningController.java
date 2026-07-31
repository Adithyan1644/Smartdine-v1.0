package com.smartdine.controller;

import com.smartdine.dto.RestaurantConfigDTO;
import com.smartdine.coreheart.Restaurant;
import com.smartdine.repository.RestaurantRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/public/provision")
@CrossOrigin(origins = "*")
public class ProvisioningController {

    private final RestaurantRepository restaurantRepository;
    private final JdbcTemplate jdbcTemplate;

    public ProvisioningController(RestaurantRepository restaurantRepository, JdbcTemplate jdbcTemplate) {
        this.restaurantRepository = restaurantRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/activate")
    public ResponseEntity<RestaurantConfigDTO> activateBiller(@RequestParam(required = false, name = "syncCode") String syncCode,
                                                               @RequestParam(required = false, name = "code") String code) {
        String effectiveCode = (syncCode != null && !syncCode.trim().isEmpty()) ? syncCode : code;
        return activate(effectiveCode);
    }

    public ResponseEntity<RestaurantConfigDTO> activate(String syncCode) {
        if (syncCode == null || syncCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Sync code cannot be empty");
        }
        Restaurant restaurant = restaurantRepository.findByBillerSyncCode(syncCode)
                .or(() -> restaurantRepository.findBySyncCodeAndIsDeletedFalse(syncCode))
                .orElseThrow(() -> new NoSuchElementException("Active Sync Code not found on server: " + syncCode));

        UUID restId = restaurant.getId() != null ? restaurant.getId() : restaurant.getRestaurantId();
        RestaurantConfigDTO dto = new RestaurantConfigDTO();
        dto.setRestaurantId(restId);
        dto.setRestaurantName(restaurant.getName());
        dto.setTest(restaurant.isTest());

        // Extract metadata cleanly using safe, structured JDBC queries
        dto.setAreas(safeQueryForList("SELECT id, name FROM areas WHERE restaurant_id = ?", restId));
        dto.setTables(safeQueryForList("SELECT id, table_number AS number, area_name AS area, capacity, status FROM dining_tables WHERE restaurant_id = ?", restId));
        dto.setMenuCategories(safeQueryForList("SELECT id, name FROM menu_categories WHERE restaurant_id = ?", restId));
        dto.setMenuItems(safeQueryForList("SELECT id, name, price, category_name AS category, is_available AS status FROM menu_items WHERE restaurant_id = ?", restId));
        dto.setModifierGroups(safeQueryForList("SELECT id, name FROM modifier_groups WHERE restaurant_id = ?", restId));

        return ResponseEntity.ok(dto);
    }

    private List<Map<String, Object>> safeQueryForList(String sql, UUID restId) {
        try {
            return jdbcTemplate.queryForList(sql, restId);
        } catch (Exception e) {
            System.err.println("Database schema metadata query bypassed: " + sql + " | Error: " + e.getMessage());
            return new ArrayList<>();
        }
    }


    @PostMapping("/report-ip")
    public ResponseEntity<String> reportLocalIp(
            @RequestHeader("X-Restaurant-ID") UUID restaurantId,
            @RequestParam("ip") String ipAddress) {
        
        Restaurant restaurant = restaurantRepository.findByRestaurantId(restaurantId)
                .or(() -> restaurantRepository.findById(restaurantId))
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        
        restaurant.setActiveLocalIp(ipAddress);
        restaurantRepository.save(restaurant);
        
        System.out.println("☁️ GCP Cloud: Registered IP [" + ipAddress + "] for Restaurant: " + restaurant.getName());
        return ResponseEntity.ok("IP Registered");
    }

    @GetMapping("/active-ip")
    public ResponseEntity<Map<String, String>> getActiveIp(@RequestParam("syncCode") String syncCode) {
        Restaurant restaurant = restaurantRepository.findBySyncCodeAndIsDeletedFalse(syncCode.trim())
                .or(() -> restaurantRepository.findByBillerSyncCode(syncCode.trim()))
                .orElseThrow(() -> new RuntimeException("Invalid Sync Code: " + syncCode));

        UUID restId = restaurant.getRestaurantId() != null ? restaurant.getRestaurantId() : restaurant.getId();
        return ResponseEntity.ok(Map.of(
            "restaurantId", restId != null ? restId.toString() : "",
            "localIp", restaurant.getActiveLocalIp() != null ? restaurant.getActiveLocalIp() : "127.0.0.1"
        ));
    }
}

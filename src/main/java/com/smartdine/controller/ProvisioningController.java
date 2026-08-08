package com.smartdine.controller;

import com.smartdine.dto.RestaurantConfigDTO;
import com.smartdine.coreheart.Restaurant;
import com.smartdine.repository.RestaurantRepository;
import com.smartdine.service.ActivationService;
import com.smartdine.config.DataSourceContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private ActivationService activationService;

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

    @GetMapping("/list-active-restaurants")
    public ResponseEntity<?> listActiveRestaurants() {
        Map<String, Object> result = new HashMap<>();
        try {
            DataSourceContextHolder.set(DataSourceContextHolder.PROD);
            List<Map<String, Object>> prodRows = jdbcTemplate.queryForList(
                "SELECT id, restaurant_id as \"restaurantId\", name, sync_code as \"syncCode\", biller_sync_code as \"billerSyncCode\", is_test as \"isTest\" FROM restaurants"
            );
            result.put("PROD", prodRows);
        } catch (Exception e) {
            result.put("PROD_error", e.getMessage());
        }

        try {
            DataSourceContextHolder.set(DataSourceContextHolder.DEV);
            List<Map<String, Object>> devRows = jdbcTemplate.queryForList(
                "SELECT id, restaurant_id as \"restaurantId\", name, sync_code as \"syncCode\", biller_sync_code as \"billerSyncCode\", is_test as \"isTest\" FROM restaurants"
            );
            result.put("DEV", devRows);
        } catch (Exception e) {
            result.put("DEV_error", e.getMessage());
        } finally {
            DataSourceContextHolder.clear();
        }
        return ResponseEntity.ok(result);
    }

    public ResponseEntity<RestaurantConfigDTO> activate(String syncCode) {
        if (syncCode == null || syncCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Sync code cannot be empty");
        }

        String codeToSearch = syncCode.trim();
        Restaurant restaurant = null;

        try {
            com.smartdine.config.DataSourceContextHolder.set(com.smartdine.config.DataSourceContextHolder.PROD);
            restaurant = restaurantRepository.findByBillerSyncCode(codeToSearch)
                    .or(() -> restaurantRepository.findBySyncCodeAndIsDeletedFalse(codeToSearch))
                    .orElse(null);
        } catch (Exception ignored) {}

        if (restaurant == null) {
            try {
                com.smartdine.config.DataSourceContextHolder.set(com.smartdine.config.DataSourceContextHolder.DEV);
                restaurant = restaurantRepository.findByBillerSyncCode(codeToSearch)
                        .or(() -> restaurantRepository.findBySyncCodeAndIsDeletedFalse(codeToSearch))
                        .orElse(null);
            } catch (Exception ignored) {}
        }

        if (restaurant == null) {
            try {
                java.util.List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, restaurant_id, name, is_test FROM restaurants WHERE sync_code = ? OR biller_sync_code = ?",
                    codeToSearch, codeToSearch
                );
                if (!rows.isEmpty()) {
                    Map<String, Object> rMap = rows.get(0);
                    restaurant = new Restaurant();
                    UUID rId = UUID.fromString(rMap.get("id").toString());
                    restaurant.setId(rId);
                    restaurant.setRestaurantId(rMap.get("restaurant_id") != null ? UUID.fromString(rMap.get("restaurant_id").toString()) : rId);
                    restaurant.setName(rMap.get("name") != null ? rMap.get("name").toString() : "Restaurant");
                    restaurant.setTest(rMap.get("is_test") != null && Boolean.parseBoolean(rMap.get("is_test").toString()));
                }
            } catch (Exception ignored) {}
        }

        if (restaurant == null) {
            com.smartdine.config.DataSourceContextHolder.clear();
            throw new NoSuchElementException("Active Sync Code not found on server: " + syncCode);
        }

        UUID id1 = restaurant.getId();
        UUID id2 = restaurant.getRestaurantId();
        UUID restId = id1 != null ? id1 : id2;

        RestaurantConfigDTO dto = new RestaurantConfigDTO();
        dto.setRestaurantId(restId);
        dto.setRestaurantName(restaurant.getName());
        dto.setTest(restaurant.isTest());
        dto.setSyncCode(restaurant.getBillerSyncCode() != null ? restaurant.getBillerSyncCode() : restaurant.getSyncCode());

        // Extract Administrator Credentials for Multi-Alias Seeding
        try {
            List<Map<String, Object>> adminRows = safeQueryForList(
                "SELECT username, password, phone, full_name FROM app_users WHERE restaurant_id = ? AND role = 'ADMIN' LIMIT 1",
                id1, id2
            );
            if (!adminRows.isEmpty()) {
                Map<String, Object> admin = adminRows.get(0);
                dto.setAdminUsername(admin.get("username") != null ? admin.get("username").toString() : null);
                dto.setAdminPasswordHash(admin.get("password") != null ? admin.get("password").toString() : null);
                dto.setAdminPhone(admin.get("phone") != null ? admin.get("phone").toString() : null);
                dto.setAdminFullName(admin.get("full_name") != null ? admin.get("full_name").toString() : null);
            }
        } catch (Exception ignored) {}

        // Extract metadata cleanly using safe, dual-aliased JDBC queries for 100% compatibility across consumers
        dto.setAreas(safeQueryForList("SELECT id, name FROM areas WHERE restaurant_id = ?", id1, id2));
        dto.setTables(safeQueryForList("SELECT id, table_number AS number, table_number AS tableNumber, area_name AS area, area_name AS areaName, capacity, status FROM dining_tables WHERE restaurant_id = ? AND (is_deleted IS NULL OR is_deleted = false)", id1, id2));
        List<Map<String, Object>> catMaps = safeQueryForList("SELECT id, name FROM menu_categories WHERE restaurant_id = ? AND (is_deleted IS NULL OR is_deleted = false)", id1, id2);
        dto.setMenuCategories(catMaps);
        List<String> catNames = catMaps.stream()
                .map(m -> m.get("name") != null ? m.get("name").toString() : "")
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
        dto.setCategories(catNames);
        dto.setMenuItems(safeQueryForList("SELECT id, name, price, short_code AS shortCode, short_code AS code, category_name AS category, category_name AS categoryName, is_available AS status, is_veg AS veg, is_veg AS isVeg FROM menu_items WHERE restaurant_id = ? AND (is_deleted IS NULL OR is_deleted = false)", id1, id2));
        dto.setModifierGroups(safeQueryForList("SELECT id, name FROM modifier_groups WHERE restaurant_id = ?", id1, id2));
        dto.setAddons(safeQueryForList("SELECT id, name, price, is_available AS available FROM addon_items WHERE restaurant_id = ?", id1, id2));

        com.smartdine.config.DataSourceContextHolder.clear();
        return ResponseEntity.ok(dto);
    }

    private List<Map<String, Object>> safeQueryForList(String sql, UUID id1, UUID id2) {
        try {
            if (id2 != null && !id2.equals(id1)) {
                sql = sql.replace("WHERE restaurant_id = ?", "WHERE (restaurant_id = ? OR restaurant_id = ?)");
                return jdbcTemplate.queryForList(sql, id1, id2);
            }
            return jdbcTemplate.queryForList(sql, id1);
        } catch (Exception e) {
            System.err.println("Database schema metadata query bypassed: " + sql + " | Error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Industry-grade write endpoint — called by the Web Admin panel to persist
     * any changes (tables, menu items, categories, waiters, addons) to Cloud SQL.
     * Uses JPA repositories via ActivationService.syncCloudConfiguration() so that
     * all column constraints are satisfied and all entities are properly handled.
     *
     * When the JavaFX POS clicks "Sync Web Config", it reads from /provision/activate
     * which reads from the same Cloud SQL — completing the bidirectional sync loop.
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/update-config")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> updateConfig(@RequestBody Map<String, Object> body) {
        try {
            String syncCode = body.get("syncCode") != null ? body.get("syncCode").toString().trim() : null;
            if (syncCode == null || syncCode.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "syncCode is required"));
            }

            // Resolve restaurant — try both sync and biller code
            Restaurant restaurant = restaurantRepository.findByBillerSyncCode(syncCode)
                    .or(() -> restaurantRepository.findBySyncCodeAndIsDeletedFalse(syncCode))
                    .orElse(null);

            if (restaurant == null) {
                // Fallback: raw query across both datasources
                List<Map<String, Object>> rows = new ArrayList<>();
                try {
                    com.smartdine.config.DataSourceContextHolder.set(com.smartdine.config.DataSourceContextHolder.PROD);
                    rows = jdbcTemplate.queryForList(
                        "SELECT id, restaurant_id, name, is_test FROM restaurants WHERE sync_code = ? OR biller_sync_code = ? LIMIT 1",
                        syncCode, syncCode);
                } catch (Exception ignored) {}
                if (rows.isEmpty()) {
                    return ResponseEntity.status(404).body(Map.of("error", "Sync code not found: " + syncCode));
                }
                Map<String, Object> rMap = rows.get(0);
                restaurant = new Restaurant();
                UUID rId = UUID.fromString(rMap.get("id").toString());
                restaurant.setId(rId);
                restaurant.setRestaurantId(rMap.get("restaurant_id") != null
                    ? UUID.fromString(rMap.get("restaurant_id").toString()) : rId);
                restaurant.setName(rMap.get("name") != null ? rMap.get("name").toString() : "Restaurant");
            }

            UUID restaurantId = restaurant.getId() != null ? restaurant.getId() : restaurant.getRestaurantId();
            com.smartdine.coreheart.TenantContext.setRestaurantId(restaurantId);

            try {
                // Build config map in the format syncCloudConfiguration expects.
                // Normalize all incoming fields to handle both Web Admin ("category") and
                // POS ("categoryName") field naming conventions.
                Map<String, Object> configMap = new HashMap<>(body);
                configMap.put("restaurantId", restaurantId.toString());

                // Normalize menuItems: ensure shortCode and categoryName are always set
                List<Map<String, Object>> rawItems = (List<Map<String, Object>>) body.get("menuItems");
                if (rawItems != null) {
                    Set<String> usedCodes = new java.util.LinkedHashSet<>();
                    List<Map<String, Object>> normalised = new ArrayList<>();
                    int counter = 1;
                    for (Map<String, Object> item : rawItems) {
                        Map<String, Object> t = new HashMap<>(item);

                        // categoryName: prefer explicit field, fall back to "category"
                        if (t.get("categoryName") == null && t.get("category") != null) {
                            t.put("categoryName", t.get("category"));
                        }

                        // shortCode: generate from name if missing
                        String sc = t.get("shortCode") != null ? t.get("shortCode").toString().trim()
                                  : t.get("code") != null ? t.get("code").toString().trim() : "";
                        if (sc.isEmpty()) {
                            String name = t.get("name") != null ? t.get("name").toString() : "ITM";
                            sc = (name.length() >= 3 ? name.substring(0, 3) : name).toUpperCase()
                                    .replaceAll("[^A-Z0-9]", "");
                            if (sc.isEmpty()) sc = "ITM";
                        }
                        while (usedCodes.contains(sc)) { sc = sc + (counter++); }
                        usedCodes.add(sc);
                        t.put("shortCode", sc);

                        // veg flag: resolve from boolean or type string
                        if (t.get("veg") == null) {
                            t.put("veg", "Non-Veg".equalsIgnoreCase(
                                t.get("type") != null ? t.get("type").toString() : "") ? false : true);
                        }

                        normalised.add(t);
                    }
                    configMap.put("menuItems", normalised);
                }

                // Sync via the proven JPA path — handles all constraints correctly
                activationService.syncCloudConfiguration(configMap);

                int tableCount = body.get("tables") != null ? ((List<?>) body.get("tables")).size() : 0;
                int menuCount  = rawItems != null ? rawItems.size() : 0;

                System.out.println("✅ [ProvisioningController] Cloud config updated for " + syncCode
                    + " — " + tableCount + " tables, " + menuCount + " menu items");

                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "syncCode", syncCode,
                    "restaurantId", restaurantId.toString(),
                    "tablesUpdated", tableCount,
                    "menuItemsUpdated", menuCount
                ));
            } finally {
                com.smartdine.coreheart.TenantContext.clear();
                com.smartdine.config.DataSourceContextHolder.clear();
            }
        } catch (Exception e) {
            System.err.println("❌ [ProvisioningController] updateConfig failed: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }


    @PostMapping(value = "/report-ip", consumes = org.springframework.http.MediaType.ALL_VALUE)
    public ResponseEntity<?> reportLocalIp(
            @RequestHeader(name = "X-Restaurant-ID", required = false) UUID restaurantIdHeader,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(name = "ip", required = false) String ipParam,
            @RequestParam(name = "restaurantId", required = false) String restIdParam) {

        UUID targetId = restaurantIdHeader;
        String targetIp = ipParam;

        if (body != null) {
            if (body.get("restaurantId") != null && !body.get("restaurantId").toString().trim().isEmpty()) {
                try { targetId = UUID.fromString(body.get("restaurantId").toString().trim()); } catch (Exception ignored) {}
            }
            if (body.get("localIp") != null) {
                targetIp = body.get("localIp").toString();
            } else if (body.get("ip") != null) {
                targetIp = body.get("ip").toString();
            }
        }
        if (targetId == null && restIdParam != null && !restIdParam.trim().isEmpty()) {
            try { targetId = UUID.fromString(restIdParam.trim()); } catch (Exception ignored) {}
        }

        if (targetId == null) {
            return ResponseEntity.ok(Map.of("success", true, "message", "IP report acknowledged"));
        }

        UUID finalId = targetId;
        Restaurant restaurant = restaurantRepository.findByRestaurantId(finalId)
                .or(() -> restaurantRepository.findById(finalId))
                .orElse(null);

        if (restaurant != null && targetIp != null) {
            restaurant.setActiveLocalIp(targetIp);
            restaurantRepository.save(restaurant);
            System.out.println("📶 [ProvisioningController] Updated active local IP to " + targetIp + " for restaurant " + restaurant.getName());
            return ResponseEntity.ok(Map.of("success", true, "localIp", targetIp));
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "IP report received"));
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

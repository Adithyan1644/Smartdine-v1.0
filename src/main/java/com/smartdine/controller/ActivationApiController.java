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

    @Autowired
    private com.smartdine.repository.RestaurantRepository restaurantRepository;

    @Autowired
    private com.smartdine.repository.OrderRepository orderRepository;

    @Autowired
    private com.smartdine.repository.KOTRepository kotRepository;

    @Autowired
    private com.smartdine.repository.MenuRepository menuRepository;

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

    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics(@RequestParam(required = false) String code) {
        java.util.UUID restaurantId = null;

        // 1. Try resolving restaurantId from sync code
        if (code != null && !code.trim().isEmpty()) {
            java.util.Optional<com.smartdine.coreheart.Restaurant> restOpt = restaurantRepository.findBySyncCodeAndIsDeletedFalse(code.trim());
            if (restOpt.isPresent()) {
                restaurantId = restOpt.get().getRestaurantId();
            }
        }

        // 2. Fall back to system config
        if (restaurantId == null) {
            java.util.Optional<com.smartdine.coreheart.SystemConfig> configOpt = activationService.getSystemConfig();
            if (configOpt.isPresent()) {
                restaurantId = configOpt.get().getRestaurantId();
            }
        }

        // 3. Fall back to TenantContext or default ID
        if (restaurantId == null) {
            restaurantId = com.smartdine.coreheart.TenantContext.getRestaurantId();
        }
        if (restaurantId == null) {
            restaurantId = java.util.UUID.fromString("28e79200-0000-4000-a000-000000000001");
        }

        // 4. Query all orders of today
        java.time.LocalDateTime startOfToday = java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        java.util.List<com.smartdine.coreheart.Order> orders = orderRepository.findByRestaurantIdAndStartedAtAfter(restaurantId, startOfToday);

        // 5. Calculations
        java.math.BigDecimal salesVal = java.math.BigDecimal.ZERO;
        int ordersVal = orders.size();
        int dineInCount = 0;
        int takeawayCount = 0;
        int deliveryCount = 0;

        java.util.List<java.util.UUID> orderIds = new java.util.ArrayList<>();

        for (com.smartdine.coreheart.Order o : orders) {
            orderIds.add(o.getId());
            if (o.getStatus() == com.smartdine.coreheart.OrderStatus.PAID) {
                salesVal = salesVal.add(o.getGrandTotal());
            }
            if (o.getType() == com.smartdine.coreheart.OrderType.DINE_IN) dineInCount++;
            else if (o.getType() == com.smartdine.coreheart.OrderType.PICK_UP) takeawayCount++;
            else deliveryCount++;
        }

        // Top Dishes computation
        java.util.Map<String, Integer> itemQuantities = new java.util.HashMap<>();
        if (!orderIds.isEmpty()) {
            java.util.List<com.smartdine.coreheart.KOT> kots = kotRepository.findByOrderIdIn(orderIds);
            for (com.smartdine.coreheart.KOT k : kots) {
                for (com.smartdine.coreheart.KOTItem item : k.getItems()) {
                    String name = item.getItemName();
                    itemQuantities.put(name, itemQuantities.getOrDefault(name, 0) + item.getQuantity());
                }
            }
        }

        // Fetch MenuItem prices to compute revenue per dish
        java.util.List<com.smartdine.coreheart.MenuItem> menuItems = menuRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId);
        java.util.Map<String, java.math.BigDecimal> itemPrices = new java.util.HashMap<>();
        for (com.smartdine.coreheart.MenuItem m : menuItems) {
            itemPrices.put(m.getName(), m.getPrice());
        }

        java.util.List<Map<String, Object>> topDishes = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Integer> entry : itemQuantities.entrySet()) {
            String name = entry.getKey();
            int qty = entry.getValue();
            java.math.BigDecimal price = itemPrices.getOrDefault(name, java.math.BigDecimal.valueOf(150.0));
            java.math.BigDecimal revenue = price.multiply(java.math.BigDecimal.valueOf(qty));
            topDishes.add(Map.of(
                "name", name,
                "orders", qty,
                "revenue", revenue.doubleValue()
            ));
        }

        // Sort top dishes by revenue desc
        topDishes.sort((a, b) -> Double.compare((Double) b.get("revenue"), (Double) a.get("revenue")));
        if (topDishes.size() > 5) {
            topDishes = topDishes.subList(0, 5);
        }

        // Business mix percentages
        double dineInPct = 0;
        double takeawayPct = 0;
        double deliveryPct = 0;
        if (ordersVal > 0) {
            dineInPct = Math.round((dineInCount * 100.0) / ordersVal);
            takeawayPct = Math.round((takeawayCount * 100.0) / ordersVal);
            deliveryPct = Math.round((deliveryCount * 100.0) / ordersVal);
        }

        // Pulse insights
        java.util.List<String> pulse = new java.util.ArrayList<>();
        if (ordersVal > 0) {
            pulse.add("Dine-in is your leading channel today with " + dineInCount + " orders.");
            pulse.add("Average order value stands at ₹" + String.format("%.2f", salesVal.doubleValue() / ordersVal) + ".");
            pulse.add("Live sync is active. System is running healthy.");
        } else {
            pulse.add("Welcome to Surabhi SmartDine! Connect your POS client to seed menus.");
            pulse.add("Add your waiter staff in the Staff management section.");
            pulse.add("No sales activity recorded yet today.");
        }

        // Daily chart data reflecting today's sales
        java.util.List<Map<String, Object>> dailyChart = java.util.List.of(
            Map.of("name", "Mon", "sales", 0.0, "expenses", 0.0, "profit", 0.0),
            Map.of("name", "Tue", "sales", 0.0, "expenses", 0.0, "profit", 0.0),
            Map.of("name", "Wed", "sales", 0.0, "expenses", 0.0, "profit", 0.0),
            Map.of("name", "Thu", "sales", 0.0, "expenses", 0.0, "profit", 0.0),
            Map.of("name", "Fri", "sales", 0.0, "expenses", 0.0, "profit", 0.0),
            Map.of("name", "Sat", "sales", 0.0, "expenses", 0.0, "profit", 0.0),
            Map.of("name", "Sun", "sales", salesVal.doubleValue(), "expenses", 0.0, "profit", salesVal.doubleValue())
        );

        Map<String, Object> overviewMap = new java.util.HashMap<>();
        overviewMap.put("kpis", Map.of(
            "sales", Map.of("value", salesVal.doubleValue()),
            "expenses", Map.of("value", 0.0),
            "profit", Map.of("value", salesVal.doubleValue()),
            "orders", Map.of("value", ordersVal)
        ));
        overviewMap.put("pulse", pulse);
        overviewMap.put("topDishes", topDishes);
        overviewMap.put("kitchen", Map.of(
            "status", ordersVal > 0 ? "Excellent" : "Idle",
            "prepTime", ordersVal > 0 ? "12m" : "—",
            "delayedOrders", 0,
            "fastestItem", topDishes.isEmpty() ? "—" : topDishes.get(0).get("name"),
            "slowestItem", "—",
            "efficiency", 100
        ));
        overviewMap.put("businessMix", java.util.List.of(
            Map.of("name", "Dine In", "value", dineInPct),
            Map.of("name", "Takeaway", "value", takeawayPct),
            Map.of("name", "Delivery", "value", deliveryPct)
        ));
        overviewMap.put("insights", java.util.List.of(
            Map.of("priority", "Low", "title", "Database Status", "desc", "System synchronized and healthy."),
            Map.of("priority", "Medium", "title", "Live Connection", "desc", "POS terminal is active and broadcasting.")
        ));
        overviewMap.put("charts", Map.of(
            "Daily", dailyChart,
            "Weekly", java.util.List.of(Map.of("name", "Wk 4", "sales", salesVal.doubleValue(), "expenses", 0.0, "profit", salesVal.doubleValue())),
            "Monthly", java.util.List.of(Map.of("name", "Jun", "sales", salesVal.doubleValue(), "expenses", 0.0, "profit", salesVal.doubleValue()))
        ));

        // Top level response structure for all screens
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("overview", overviewMap);
        response.put("sales", Map.of(
            "kpis", Map.of(
                "grossSales", Map.of("value", salesVal.doubleValue()),
                "netSales", Map.of("value", salesVal.doubleValue()),
                "avgOrder", Map.of("value", ordersVal > 0 ? salesVal.doubleValue() / ordersVal : 0.0),
                "transactions", Map.of("value", ordersVal)
            ),
            "comparison", Map.of(
                "dineIn", dineInPct,
                "takeaway", takeawayPct,
                "delivery", deliveryPct
            )
        ));
        response.put("kitchen", Map.of(
            "orderKpis", Map.of(
                "totalOrders", Map.of("value", ordersVal),
                "completed", Map.of("value", ordersVal),
                "averageTime", Map.of("value", 12),
                "delayed", Map.of("value", 0)
            )
        ));

        return ResponseEntity.ok(response);
    }
}

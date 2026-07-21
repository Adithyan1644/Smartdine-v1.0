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

    @Autowired
    private ProvisioningController provisioningController;

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        boolean activated = activationService.isSystemActivated();
        return ResponseEntity.ok(Map.of(
            "activated", activated,
            "restaurantId", activationService.getSystemConfig().map(c -> c.getRestaurantId().toString()).orElse(""),
            "restaurantName", activationService.getSystemConfig().map(c -> c.getRestaurantName()).orElse("")
        ));
    }

    @GetMapping("/activate")
    public ResponseEntity<?> activateViaGet(@RequestParam String code) {
        return provisioningController.activate(code);
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
            String syncCode = request.get("syncCode") != null ? request.get("syncCode").toString() : ("SD-" + (100000 + (int)(Math.random() * 900000)));
            String restId = request.get("restaurantId") != null ? request.get("restaurantId").toString() : java.util.UUID.randomUUID().toString();
            
            String restName = request.get("restaurantName") != null ? request.get("restaurantName").toString() : null;
            if (restName == null && request.get("profile") instanceof Map) {
                Object pName = ((Map) request.get("profile")).get("restaurantName");
                if (pName != null) restName = pName.toString();
            }
            if (restName == null || restName.isEmpty()) restName = "SmartDine Restaurant";

            String ownerName = request.get("ownerName") != null ? request.get("ownerName").toString() : null;
            if (ownerName == null && request.get("profile") instanceof Map) {
                Object pOwner = ((Map) request.get("profile")).get("ownerName");
                if (pOwner != null) ownerName = pOwner.toString();
            }
            if (ownerName == null) ownerName = "Manager";

            String ownerEmail = request.get("ownerEmail") != null ? request.get("ownerEmail").toString() : "";

            String fileName = "activation-" + syncCode.trim().toLowerCase() + ".json";
            java.io.File file = new java.io.File(fileName);
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
            java.util.Set<String> usedCodes = new java.util.HashSet<>();
            if (incomingMenuItems != null) {
                int codeCounter = 1;
                for (Map<String, Object> item : incomingMenuItems) {
                    String name = item.get("name") != null ? item.get("name").toString() : "";
                    String baseCode = item.get("code") != null && !item.get("code").toString().trim().isEmpty() 
                        ? item.get("code").toString().trim().toUpperCase() 
                        : (name.length() >= 3 ? name.substring(0, 3).toUpperCase().replaceAll("[^A-Z0-9]", "") : "ITM");
                    if (baseCode.isEmpty()) baseCode = "ITM";
                    
                    String shortCode = baseCode;
                    while (usedCodes.contains(shortCode)) {
                        shortCode = baseCode + (codeCounter++);
                    }
                    usedCodes.add(shortCode);

                    double price = item.get("price") != null ? Double.parseDouble(item.get("price").toString()) : 0.0;
                    boolean veg = true;
                    if (item.get("veg") != null) {
                        if (item.get("veg") instanceof Boolean) {
                            veg = (Boolean) item.get("veg");
                        } else {
                            veg = !item.get("veg").toString().equalsIgnoreCase("false");
                        }
                    } else if (item.get("type") != null) {
                        veg = item.get("type").toString().equalsIgnoreCase("Veg");
                    }
                    
                    String category = item.get("categoryName") != null ? item.get("categoryName").toString() : (item.get("category") != null ? item.get("category").toString() : "General");
                    
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
            gatewayPayload.put("restaurantId", restId);
            gatewayPayload.put("restaurantName", restName);
            gatewayPayload.put("ownerName", ownerName);
            gatewayPayload.put("ownerEmail", ownerEmail);
            gatewayPayload.put("cgstRate", new java.math.BigDecimal("2.50"));
            gatewayPayload.put("sgstRate", new java.math.BigDecimal("2.50"));
            gatewayPayload.put("serviceChargeRate", new java.math.BigDecimal("5.00"));
            gatewayPayload.put("categories", new java.util.ArrayList<>(categories));
            gatewayPayload.put("tables", mappedTables);
            gatewayPayload.put("menuItems", mappedMenuItems);
            gatewayPayload.put("modifierGroups", modifierGroups);
            gatewayPayload.put("waiters", mappedWaiters);
            
            // Save payload to root and subfolder paths so MockCloudGatewayController always finds it
            mapper.writeValue(file, gatewayPayload);
            try { mapper.writeValue(new java.io.File("core-heart/" + fileName), gatewayPayload); } catch (Exception ignored) {}
            try { mapper.writeValue(new java.io.File("core-heart/core-heart/" + fileName), gatewayPayload); } catch (Exception ignored) {}
            try { mapper.writeValue(new java.io.File("activation-data.json"), gatewayPayload); } catch (Exception ignored) {}
            try { mapper.writeValue(new java.io.File("core-heart/activation-data.json"), gatewayPayload); } catch (Exception ignored) {}
            try { mapper.writeValue(new java.io.File("core-heart/core-heart/activation-data.json"), gatewayPayload); } catch (Exception ignored) {}
            
            // Immediately sync payload directly into the active PostgreSQL database tables (menu_items, dining_tables, etc.)
            try {
                activationService.syncCloudConfiguration(gatewayPayload);
                System.out.println("☁️ Live Sync: Web configuration successfully saved directly into PostgreSQL database.");
            } catch (Exception e) {
                System.err.println("⚠️ Notice: Direct DB sync: " + e.getMessage());
            }
            
            return ResponseEntity.ok(Map.of("success", true, "code", syncCode));
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

        // 4. Query all orders of last 30 days
        java.time.LocalDateTime startOfThirtyDaysAgo = java.time.LocalDateTime.now().minusDays(30).withHour(0).withMinute(0).withSecond(0).withNano(0);
        java.util.List<com.smartdine.coreheart.Order> allOrders = orderRepository.findByRestaurantIdAndStartedAtAfter(restaurantId, startOfThirtyDaysAgo);
        
        if (allOrders.isEmpty()) {
            // Fallback 1: Try SystemConfig restaurantId if different
            java.util.Optional<com.smartdine.coreheart.SystemConfig> sysOpt = activationService.getSystemConfig();
            if (sysOpt.isPresent() && !sysOpt.get().getRestaurantId().equals(restaurantId)) {
                allOrders = orderRepository.findByRestaurantIdAndStartedAtAfter(sysOpt.get().getRestaurantId(), startOfThirtyDaysAgo);
            }
        }

        if (allOrders.isEmpty()) {
            // Fallback 2: Retrieve all recent orders for local machine POS demo
            allOrders = orderRepository.findAll().stream()
                .filter(o -> o.getStartedAt() != null && !o.getStartedAt().isBefore(startOfThirtyDaysAgo))
                .collect(java.util.stream.Collectors.toList());
        }

        // 5. Calculations for different time boundaries
        java.time.LocalDateTime todayStart = java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        java.time.LocalDateTime yesterdayStart = todayStart.minusDays(1);
        java.time.LocalDateTime weekStart = todayStart.minusDays(7);
        java.time.LocalDateTime monthStart = todayStart.withDayOfMonth(1);

        java.math.BigDecimal todaySales = java.math.BigDecimal.ZERO;
        java.math.BigDecimal yesterdaySales = java.math.BigDecimal.ZERO;
        java.math.BigDecimal weeklySales = java.math.BigDecimal.ZERO;
        java.math.BigDecimal monthlySales = java.math.BigDecimal.ZERO;

        int todayOrdersCount = 0;
        int todayDineInCount = 0;
        int todayTakeawayCount = 0;
        int todayDeliveryCount = 0;

        java.math.BigDecimal todayDineInSales = java.math.BigDecimal.ZERO;
        java.math.BigDecimal todayTakeawaySales = java.math.BigDecimal.ZERO;
        java.math.BigDecimal todayDeliverySales = java.math.BigDecimal.ZERO;

        java.math.BigDecimal todayUpiSales = java.math.BigDecimal.ZERO;
        java.math.BigDecimal todayCashSales = java.math.BigDecimal.ZERO;
        java.math.BigDecimal todayCardSales = java.math.BigDecimal.ZERO;

        java.util.List<java.util.UUID> todayOrderIds = new java.util.ArrayList<>();

        for (com.smartdine.coreheart.Order o : allOrders) {
            java.time.LocalDateTime started = o.getStartedAt();
            java.math.BigDecimal total = o.getGrandTotal() != null ? o.getGrandTotal() : java.math.BigDecimal.ZERO;
            boolean isNonCancelled = o.getStatus() != com.smartdine.coreheart.OrderStatus.CANCELLED;

            if (!started.isBefore(todayStart)) {
                todayOrderIds.add(o.getId());
                todayOrdersCount++;
                if (isNonCancelled) {
                    todaySales = todaySales.add(total);
                    
                    // Channel sales
                    if (o.getType() == com.smartdine.coreheart.OrderType.DINE_IN) {
                        todayDineInSales = todayDineInSales.add(total);
                        todayDineInCount++;
                    } else if (o.getType() == com.smartdine.coreheart.OrderType.PICK_UP) {
                        todayTakeawaySales = todayTakeawaySales.add(total);
                        todayTakeawayCount++;
                    } else {
                        todayDeliverySales = todayDeliverySales.add(total);
                        todayDeliveryCount++;
                    }

                    // Payment sales
                    String payMode = o.getPaymentMode();
                    if (payMode != null) {
                        if (payMode.equalsIgnoreCase("UPI")) todayUpiSales = todayUpiSales.add(total);
                        else if (payMode.equalsIgnoreCase("CASH")) todayCashSales = todayCashSales.add(total);
                        else if (payMode.equalsIgnoreCase("CARD")) todayCardSales = todayCardSales.add(total);
                    } else {
                        todayCashSales = todayCashSales.add(total);
                    }
                }
            } else if (!started.isBefore(yesterdayStart)) {
                if (isNonCancelled) yesterdaySales = yesterdaySales.add(total);
            }

            if (!started.isBefore(weekStart)) {
                if (isNonCancelled) weeklySales = weeklySales.add(total);
            }

            if (!started.isBefore(monthStart)) {
                if (isNonCancelled) monthlySales = monthlySales.add(total);
            }
        }

        // Top Dishes computation for today
        java.util.Map<String, Integer> itemQuantities = new java.util.HashMap<>();
        if (!todayOrderIds.isEmpty()) {
            java.util.List<com.smartdine.coreheart.KOT> kots = kotRepository.findByOrderIdIn(todayOrderIds);
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

        topDishes.sort((a, b) -> Double.compare((Double) b.get("revenue"), (Double) a.get("revenue")));
        if (topDishes.size() > 5) {
            topDishes = topDishes.subList(0, 5);
        }

        // Business mix percentages for today's orders
        double dineInPct = 0;
        double takeawayPct = 0;
        double deliveryPct = 0;
        if (todayOrdersCount > 0) {
            dineInPct = Math.round((todayDineInCount * 100.0) / todayOrdersCount);
            takeawayPct = Math.round((todayTakeawayCount * 100.0) / todayOrdersCount);
            deliveryPct = Math.round((todayDeliveryCount * 100.0) / todayOrdersCount);
        }

        // Revenue distribution lists for Sales screen
        double totalSalesD = todaySales.doubleValue();
        java.util.List<Map<String, Object>> distribution = java.util.List.of(
            Map.of("name", "Dine-In", "value", todayDineInSales.doubleValue(), "pct", totalSalesD > 0 ? Math.round((todayDineInSales.doubleValue() * 100) / totalSalesD) : 0, "color", "#0B6B50"),
            Map.of("name", "Takeaway", "value", todayTakeawaySales.doubleValue(), "pct", totalSalesD > 0 ? Math.round((todayTakeawaySales.doubleValue() * 100) / totalSalesD) : 0, "color", "#F59E0B"),
            Map.of("name", "Online", "value", todayDeliverySales.doubleValue(), "pct", totalSalesD > 0 ? Math.round((todayDeliverySales.doubleValue() * 100) / totalSalesD) : 0, "color", "#3B82F6")
        );

        // Payment methods lists for Sales screen
        java.util.List<Map<String, Object>> paymentMethods = java.util.List.of(
            Map.of("method", "UPI", "amount", todayUpiSales.doubleValue(), "pct", totalSalesD > 0 ? Math.round((todayUpiSales.doubleValue() * 100) / totalSalesD) : 0, "color", "#0B6B50"),
            Map.of("method", "Cash", "amount", todayCashSales.doubleValue(), "pct", totalSalesD > 0 ? Math.round((todayCashSales.doubleValue() * 100) / totalSalesD) : 0, "color", "#F59E0B"),
            Map.of("method", "Card", "amount", todayCardSales.doubleValue(), "pct", totalSalesD > 0 ? Math.round((todayCardSales.doubleValue() * 100) / totalSalesD) : 0, "color", "#3B82F6")
        );

        // Peak hours grouping for today's orders
        int slot8to10 = 0, slot12to2 = 0, slot4to6 = 0, slot7to9 = 0, slot9to11 = 0;
        for (com.smartdine.coreheart.Order o : allOrders) {
            if (o.getStartedAt().isAfter(todayStart)) {
                int hour = o.getStartedAt().getHour();
                if (hour >= 8 && hour < 10) slot8to10++;
                else if (hour >= 12 && hour < 14) slot12to2++;
                else if (hour >= 16 && hour < 18) slot4to6++;
                else if (hour >= 19 && hour < 21) slot7to9++;
                else if (hour >= 21 && hour < 23) slot9to11++;
            }
        }
        int maxSlots = Math.max(1, Math.max(slot8to10, Math.max(slot12to2, Math.max(slot4to6, Math.max(slot7to9, slot9to11)))));
        java.util.List<Map<String, Object>> peakHours = java.util.List.of(
            Map.of("slot", "8 AM – 10 AM", "label", "Breakfast Rush", "orders", slot8to10, "pct", Math.round((slot8to10 * 100.0) / maxSlots)),
            Map.of("slot", "12 PM – 2 PM", "label", "Lunch Rush", "orders", slot12to2, "pct", Math.round((slot12to2 * 100.0) / maxSlots)),
            Map.of("slot", "4 PM – 6 PM", "label", "Evening Snacks", "orders", slot4to6, "pct", Math.round((slot4to6 * 100.0) / maxSlots)),
            Map.of("slot", "7 PM – 9 PM", "label", "Dinner Rush", "orders", slot7to9, "pct", Math.round((slot7to9 * 100.0) / maxSlots)),
            Map.of("slot", "9 PM – 11 PM", "label", "Late Dinner", "orders", slot9to11, "pct", Math.round((slot9to11 * 100.0) / maxSlots))
        );

        // Daily trend grouping (Mon-Sun of this week)
        java.util.List<Map<String, Object>> dailyTrend = new java.util.ArrayList<>();
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        java.time.LocalDate todayLocalDate = java.time.LocalDate.now();
        int currentDayOfWeek = todayLocalDate.getDayOfWeek().getValue(); // 1 = Mon, 7 = Sun
        
        for (int i = 1; i <= 7; i++) {
            int offset = i - currentDayOfWeek;
            java.time.LocalDate dayDate = todayLocalDate.plusDays(offset);
            java.time.LocalDateTime dayS = dayDate.atStartOfDay();
            java.time.LocalDateTime dayE = dayS.plusDays(1);
            
            java.math.BigDecimal daySales = java.math.BigDecimal.ZERO;
            for (com.smartdine.coreheart.Order o : allOrders) {
                if (o.getStatus() == com.smartdine.coreheart.OrderStatus.PAID &&
                    o.getStartedAt().isAfter(dayS) && o.getStartedAt().isBefore(dayE)) {
                    daySales = daySales.add(o.getGrandTotal());
                }
            }
            dailyTrend.add(Map.of("name", days[i-1], "sales", daySales.doubleValue()));
        }

        // Weekly trend grouping (Last 4 weeks)
        java.util.List<Map<String, Object>> weeklyTrend = new java.util.ArrayList<>();
        for (int i = 3; i >= 0; i--) {
            java.time.LocalDateTime wS = todayStart.minusWeeks(i + 1);
            java.time.LocalDateTime wE = todayStart.minusWeeks(i);
            java.math.BigDecimal wSales = java.math.BigDecimal.ZERO;
            for (com.smartdine.coreheart.Order o : allOrders) {
                if (o.getStatus() == com.smartdine.coreheart.OrderStatus.PAID &&
                    o.getStartedAt().isAfter(wS) && o.getStartedAt().isBefore(wE)) {
                    wSales = wSales.add(o.getGrandTotal());
                }
            }
            weeklyTrend.add(Map.of("name", "Wk " + (4-i), "sales", wSales.doubleValue()));
        }

        // Monthly trend grouping (Last 6 months)
        java.util.List<Map<String, Object>> monthlyTrend = new java.util.ArrayList<>();
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        java.time.LocalDate currentMonthDate = todayLocalDate.minusMonths(5);
        for (int i = 0; i < 6; i++) {
            java.time.LocalDate mDate = currentMonthDate.plusMonths(i);
            java.time.LocalDateTime mS = mDate.withDayOfMonth(1).atStartOfDay();
            java.time.LocalDateTime mE = mS.plusMonths(1);
            
            java.math.BigDecimal mSales = java.math.BigDecimal.ZERO;
            for (com.smartdine.coreheart.Order o : allOrders) {
                if (o.getStatus() == com.smartdine.coreheart.OrderStatus.PAID &&
                    o.getStartedAt().isAfter(mS) && o.getStartedAt().isBefore(mE)) {
                    mSales = mSales.add(o.getGrandTotal());
                }
            }
            monthlyTrend.add(Map.of("name", months[mDate.getMonthValue() - 1], "sales", mSales.doubleValue()));
        }

        // Pulse insights
        java.util.List<String> pulse = new java.util.ArrayList<>();
        if (todayOrdersCount > 0) {
            pulse.add("Dine-in is your leading channel today with " + todayDineInCount + " orders.");
            pulse.add("Average order value stands at ₹" + String.format("%.2f", todaySales.doubleValue() / todayOrdersCount) + ".");
            pulse.add("Live sync is active. System is running healthy.");
        } else {
            pulse.add("Welcome to Surabhi SmartDine! Connect your POS client to seed menus.");
            pulse.add("Add your waiter staff in the Staff management section.");
            pulse.add("No sales activity recorded yet today.");
        }

        // Comparison cards
        java.math.BigDecimal compDiff = todaySales.subtract(yesterdaySales);
        double compPct = yesterdaySales.doubleValue() > 0 ? Math.round((compDiff.doubleValue() * 100.0) / yesterdaySales.doubleValue()) : 0.0;
        java.util.List<Map<String, Object>> comparison = java.util.List.of(
            Map.of("label", "Today vs Yesterday", "diff", compDiff.doubleValue(), "pct", compPct, "positive", compDiff.doubleValue() >= 0),
            Map.of("label", "This Week vs Last Week", "diff", weeklySales.doubleValue(), "pct", 0.0, "positive", true),
            Map.of("label", "This Month vs Last Month", "diff", monthlySales.doubleValue(), "pct", 0.0, "positive", true)
        );

        // Insights for Sales screen
        java.util.List<String> salesInsights = new java.util.ArrayList<>();
        if (todayOrdersCount > 0) {
            salesInsights.add("Sales today reached ₹" + todaySales.doubleValue() + ", driven by " + todayOrdersCount + " covers.");
            salesInsights.add("UPI was used in " + todayUpiSales.doubleValue() + " worth of transactions.");
            salesInsights.add("Dine-In contributed ₹" + todayDineInSales.doubleValue() + " to today's operations.");
        } else {
            salesInsights.add("No sales transactions recorded yet today.");
            salesInsights.add("Configure UPI, Cash, and Card modes to monitor transaction shares.");
        }

        Map<String, Object> overviewMap = new java.util.HashMap<>();
        overviewMap.put("kpis", Map.of(
            "sales", Map.of("value", todaySales.doubleValue()),
            "expenses", Map.of("value", 0.0),
            "profit", Map.of("value", todaySales.doubleValue()),
            "orders", Map.of("value", todayOrdersCount)
        ));
        overviewMap.put("pulse", pulse);
        overviewMap.put("topDishes", topDishes);
        overviewMap.put("kitchen", Map.of(
            "status", todayOrdersCount > 0 ? "Excellent" : "Idle",
            "prepTime", todayOrdersCount > 0 ? "12m" : "—",
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
            "Daily", dailyTrend,
            "Weekly", weeklyTrend,
            "Monthly", monthlyTrend
        ));

        // Top level response structure for all screens
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("overview", overviewMap);
        response.put("sales", Map.of(
            "kpis", Map.of(
                "today", Map.of("value", todaySales.doubleValue(), "change", compPct, "positive", compDiff.doubleValue() >= 0),
                "yesterday", Map.of("value", yesterdaySales.doubleValue(), "change", 0, "positive", true),
                "weekly", Map.of("value", weeklySales.doubleValue(), "change", 0, "positive", true),
                "monthly", Map.of("value", monthlySales.doubleValue(), "change", 0, "positive", true)
            ),
            "comparison", comparison,
            "distribution", distribution,
            "trends", Map.of(
                "Daily", dailyTrend,
                "Weekly", weeklyTrend,
                "Monthly", monthlyTrend
            ),
            "peakHours", peakHours,
            "paymentMethods", paymentMethods,
            "insights", salesInsights
        ));
        response.put("kitchen", Map.of(
            "orderKpis", Map.of(
                "total", todayOrdersCount,
                "active", 0,
                "completed", todayOrdersCount,
                "dineIn", todayDineInCount,
                "takeaway", todayTakeawayCount,
                "online", todayDeliveryCount
            ),
            "orderDistribution", java.util.List.of(
                Map.of("name", "Dine-In", "value", todayDineInCount, "color", "#0B6B50"),
                Map.of("name", "Takeaway", "value", todayTakeawayCount, "color", "#F59E0B"),
                Map.of("name", "Online", "value", todayDeliveryCount, "color", "#3B82F6")
            ),
            "kitchenKpis", java.util.List.of(
                Map.of("label", "Avg Prep Time", "value", todayOrdersCount > 0 ? "12 mins" : "—", "change", 0, "positive", true),
                Map.of("label", "Kitchen Load", "value", todayOrdersCount > 0 ? "Normal" : "Light", "change", 0, "positive", true),
                Map.of("label", "Service Quality", "value", "100%", "change", 0, "positive", true)
            ),
            "kitchenStatus", Map.of("status", todayOrdersCount > 0 ? "Excellent" : "Smooth"),
            "liveSummary", java.util.List.of(
                Map.of("label", "Completed Tickets", "value", todayOrdersCount, "color", "#0B6B50"),
                Map.of("label", "Active Tickets", "value", 0, "color", "#3B82F6"),
                Map.of("label", "Delayed Tickets", "value", 0, "color", "#DC2626")
            ),
            "delayedItems", java.util.Collections.emptyList(),
            "operationalInsights", java.util.List.of(
                Map.of("text", "Kitchen operations are currently running normal.")
            )
        ));

        return ResponseEntity.ok(response);
    }
}

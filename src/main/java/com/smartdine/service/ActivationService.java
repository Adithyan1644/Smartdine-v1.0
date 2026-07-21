package com.smartdine.service;

import com.smartdine.coreheart.*;
import com.smartdine.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import jakarta.annotation.PostConstruct;


import java.math.BigDecimal;
import java.util.*;

@Service
public class ActivationService {

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private ModifierGroupRepository modifierGroupRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private KOTRepository kotRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private com.smartdine.repository.RestaurantRepository restaurantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MdnsService mdnsService;

    @Autowired(required = false)
    private CloudIpReporter cloudIpReporter;

    @PostConstruct
    public void initActiveTenant() {
        try {
            systemConfigRepository.findAll().stream().findFirst().ifPresent(config -> {
                TenantContext.setActiveRestaurantId(config.getRestaurantId());
                System.out.println("🚀 [ActivationService] Initialized active restaurant tenant ID: " + config.getRestaurantId());
            });
        } catch (Exception e) {
            System.err.println("⚠️ [ActivationService] Failed to initialize active tenant: " + e.getMessage());
        }
    }

    /**
     * Checks if the system is already activated.
     */
    public boolean isSystemActivated() {
        return systemConfigRepository.findAll().stream()
                .anyMatch(SystemConfig::isActivated);
    }


    /**
     * Retrieves the current system configuration.
     */
    public Optional<SystemConfig> getSystemConfig() {
        return systemConfigRepository.findAll().stream().findFirst();
    }

    /**
     * Executes the cloud handshake, pulls config, seeds the database.
     */
    @Transactional
    public Map<String, Object> activateSystem(String activationCode, String gatewayUrl) throws Exception {
        if (activationCode == null || activationCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Activation code cannot be empty");
        }
        if (gatewayUrl == null || gatewayUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Cloud Gateway URL cannot be empty");
        }

        // 1. Fetch Configuration from Cloud Gateway (with sanitized URL path)
        String base = gatewayUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String url;
        if (base.endsWith("/activate")) {
            url = base + "?code=" + activationCode.trim();
        } else {
            url = base + "/activate?code=" + activationCode.trim();
        }
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> config;
        try {
            config = restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reach Cloud Gateway: " + e.getMessage(), e);
        }

        if (config == null || config.containsKey("error")) {
            String errorMsg = config != null ? (String) config.get("error") : "Unknown gateway error";
            throw new RuntimeException("Cloud Gateway rejected activation: " + errorMsg);
        }

        // 2. Parse basic fields
        UUID restaurantId = UUID.fromString((String) config.get("restaurantId"));
        String restaurantName = (String) config.get("restaurantName");
        
        BigDecimal cgstRate = config.get("cgstRate") != null 
                ? new BigDecimal(config.get("cgstRate").toString()) 
                : new BigDecimal("2.50");
        BigDecimal sgstRate = config.get("sgstRate") != null 
                ? new BigDecimal(config.get("sgstRate").toString()) 
                : new BigDecimal("2.50");
        BigDecimal serviceChargeRate = config.get("serviceChargeRate") != null 
                ? new BigDecimal(config.get("serviceChargeRate").toString()) 
                : new BigDecimal("0.00");

        // Set tenant context for this bootstrap run
        TenantContext.setRestaurantId(restaurantId);

        try {
            // 3. Preserve waiter accounts added via the admin panel (role = WAITER)
            // so that manually-added staff survive re-activation.
            List<AppUser> existingWaiters = userRepository
                .findByRestaurantIdAndRoleAndIsActiveTrue(restaurantId, UserRole.WAITER);

            // Purge operational data only — NOT waiter accounts
            kotRepository.deleteAll();
            orderRepository.deleteAll();
            customerRepository.deleteAll();
            menuRepository.deleteAll();
            modifierGroupRepository.deleteAll();
            categoryRepository.deleteAll();
            tableRepository.deleteAll();
            // Delete only non-waiter users (kitchen, admin seed accounts)
            userRepository.findAll().stream()
                .filter(u -> u.getRole() != UserRole.WAITER)
                .forEach(userRepository::delete);
            systemConfigRepository.deleteAll();

            // Flush all deletions
            kotRepository.flush();
            orderRepository.flush();
            customerRepository.flush();
            menuRepository.flush();
            modifierGroupRepository.flush();
            categoryRepository.flush();
            tableRepository.flush();
            userRepository.flush();
            systemConfigRepository.flush();

            // 4. Save new SystemConfig
            SystemConfig sysConfig = new SystemConfig();
            sysConfig.setActivated(true);
            sysConfig.setRestaurantId(restaurantId);
            sysConfig.setRestaurantName(restaurantName);
            sysConfig.setActivationCode(activationCode);
            sysConfig.setCgstRate(cgstRate);
            sysConfig.setSgstRate(sgstRate);
            sysConfig.setServiceChargeRate(serviceChargeRate);
            systemConfigRepository.save(sysConfig);
            TenantContext.setActiveRestaurantId(restaurantId);

            // 4b. Re-create the Restaurant record so sync-code lookups work correctly.
            // Delete old restaurant row if it exists so we can update the immutable restaurantId.
            restaurantRepository.findBySyncCodeAndIsDeletedFalse(activationCode.trim())
                .ifPresent(r -> {
                    restaurantRepository.delete(r);
                    restaurantRepository.flush();
                });

            com.smartdine.coreheart.Restaurant restaurantRecord = new com.smartdine.coreheart.Restaurant();
            restaurantRecord.setName(restaurantName);
            restaurantRecord.setSyncCode(activationCode.trim());
            restaurantRecord.setActive(true);
            restaurantRecord.setRestaurantId(restaurantId);
            restaurantRepository.save(restaurantRecord);
            System.out.println("✅ [ActivationService] Restaurant row upserted with syncCode=" + activationCode.trim() + ", restaurantId=" + restaurantId);

            // Trigger GCP IP Reporting immediately on activation
            if (cloudIpReporter != null) {
                Thread.ofVirtual().start(cloudIpReporter::reportIpToCloud);
            }

            // 5. Seed Tables
            List<Map<String, Object>> tableList = (List<Map<String, Object>>) config.get("tables");
            if (tableList != null) {
                for (Map<String, Object> tbl : tableList) {
                    DiningTable table = new DiningTable();
                    table.setRestaurantId(restaurantId);
                    table.setTableNumber((String) tbl.get("tableNumber"));
                    table.setCapacity((Integer) tbl.get("capacity"));
                    table.setAreaName((String) tbl.get("areaName"));
                    table.setStatus(TableStatus.AVAILABLE);
                    tableRepository.save(table);
                }
            }

            // 6. Seed Categories
            List<String> catList = (List<String>) config.get("categories");
            Map<String, Category> categoryMap = new HashMap<>();
            if (catList != null) {
                for (String catName : catList) {
                    Category cat = new Category();
                    cat.setRestaurantId(restaurantId);
                    cat.setName(catName);
                    cat = categoryRepository.save(cat);
                    categoryMap.put(catName, cat);
                }
            }

            // 7. Seed Modifier Groups
            List<Map<String, Object>> modifierGroups = (List<Map<String, Object>>) config.get("modifierGroups");
            Map<String, ModifierGroup> groupMap = new HashMap<>();
            if (modifierGroups != null) {
                for (Map<String, Object> grp : modifierGroups) {
                    ModifierGroup group = new ModifierGroup();
                    group.setRestaurantId(restaurantId);
                    group.setName((String) grp.get("name"));
                    group.setGlobal((Boolean) grp.get("isGlobal"));

                    List<Map<String, Object>> optionsList = (List<Map<String, Object>>) grp.get("options");
                    List<ModifierOption> options = new ArrayList<>();
                    if (optionsList != null) {
                        for (Map<String, Object> optMap : optionsList) {
                            ModifierOption opt = new ModifierOption();
                            opt.setRestaurantId(restaurantId);
                            opt.setName((String) optMap.get("name"));
                            opt.setPrice(new BigDecimal(optMap.get("price").toString()));
                            options.add(opt);
                        }
                    }
                    group.setOptions(options);
                    ModifierGroup savedGroup = modifierGroupRepository.save(group);
                    groupMap.put(savedGroup.getName(), savedGroup);
                }
            }

            // 8. Seed Menu Items
            List<Map<String, Object>> itemsList = (List<Map<String, Object>>) config.get("menuItems");
            if (itemsList != null) {
                for (Map<String, Object> itm : itemsList) {
                    MenuItem item = new MenuItem();
                    item.setRestaurantId(restaurantId);
                    item.setName((String) itm.get("name"));
                    item.setShortCode((String) itm.get("shortCode"));
                    
                    Object priceObj = itm.get("price");
                    BigDecimal price = priceObj != null ? new BigDecimal(priceObj.toString()) : BigDecimal.ZERO;
                    item.setPrice(price);

                    boolean isVeg = true;
                    if (itm.get("veg") != null) {
                        if (itm.get("veg") instanceof Boolean) {
                            isVeg = (Boolean) itm.get("veg");
                        } else {
                            isVeg = !itm.get("veg").toString().equalsIgnoreCase("false");
                        }
                    } else if (itm.get("type") != null) {
                        isVeg = itm.get("type").toString().equalsIgnoreCase("Veg");
                    }
                    item.setVeg(isVeg);
                    item.setAvailable(true);

                    String catName = (String) itm.get("categoryName");
                    Category category = categoryMap.get(catName);
                    item.setCategory(category);
                    item.setCategoryName(catName);

                    // If it is Main Course, link Al-Faham Sides modifier group (if present)
                    if ("Main Course".equals(catName) && groupMap.containsKey("Al-Faham Sides")) {
                        item.setModifierGroup(groupMap.get("Al-Faham Sides"));
                    }

                    menuRepository.save(item);
                }
            }

            // 9. Seed Waiter accounts from config (only if not already in DB from admin panel)
            List<Map<String, Object>> waitersList = (List<Map<String, Object>>) config.get("waiters");
            // Collect pins already in DB so we don't create duplicates
            Set<String> existingPins = existingWaiters.stream()
                .map(AppUser::getPin)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

            if (waitersList != null && !waitersList.isEmpty()) {
                for (Map<String, Object> w : waitersList) {
                    String pin = (String) w.get("pin");
                    if (pin == null || pin.trim().isEmpty()) continue;
                    String targetUsername = "waiter_" + pin.trim();
                    if (existingPins.contains(pin.trim()) || userRepository.findByUsername(targetUsername).isPresent()) {
                        // Waiter with this PIN or username was already added — skip seeding
                        continue;
                    }
                    AppUser customWaiter = new AppUser();
                    customWaiter.setRestaurantId(restaurantId);
                    customWaiter.setUsername(targetUsername);
                    customWaiter.setPassword(passwordEncoder.encode("waiter123"));
                    customWaiter.setRole(UserRole.WAITER);
                    customWaiter.setFullName((String) w.get("name"));
                    customWaiter.setPin(pin.trim());
                    String status = (String) w.get("status");
                    customWaiter.setActive(status == null || status.equalsIgnoreCase("Active"));
                    userRepository.save(customWaiter);
                }
            }

            // Kitchen staff
            AppUser kitchen = userRepository.findByUsername("kitchen").orElse(null);
            if (kitchen == null) {
                kitchen = new AppUser();
                kitchen.setRestaurantId(restaurantId);
                kitchen.setUsername("kitchen");
                kitchen.setPassword(passwordEncoder.encode("kitchen123"));
                kitchen.setRole(UserRole.KITCHEN);
            }
            kitchen.setFullName("Main Kitchen");
            kitchen.setPin("5050");
            kitchen.setActive(true);
            userRepository.save(kitchen);

            return config;
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Synchronizes menu and tables from the SaaS web dashboard.
     */
    @Transactional
    public void syncMenuAndTables(String activationCode) throws Exception {
        if (activationCode == null || activationCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Activation code cannot be empty");
        }

        // Fetch configuration from the local mock-cloud gateway (Spring Boot)
        String url = "http://localhost:8080/api/mock-cloud/activate?code=" + activationCode.trim();
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> config;
        try {
            config = restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch config from cloud gateway: " + e.getMessage(), e);
        }

        if (config == null || config.containsKey("error")) {
            String errorMsg = config != null ? (String) config.get("error") : "Unknown server error";
            throw new RuntimeException("API Server sync rejected: " + errorMsg);
        }

        syncCloudConfiguration(config);
    }

    @Transactional
    public void syncCloudConfiguration(Map<String, Object> config) {
        if (config == null || !config.containsKey("restaurantId")) return;
        UUID restaurantId = UUID.fromString((String) config.get("restaurantId"));
        TenantContext.setRestaurantId(restaurantId);

        try {
            // 1. Sync Tables
            List<Map<String, Object>> tableList = (List<Map<String, Object>>) config.get("tables");
            if (tableList != null) {
                List<DiningTable> existingTables = tableRepository.findByRestaurantId(restaurantId);
                Set<String> incomingTableNums = new HashSet<>();
                
                for (Map<String, Object> tbl : tableList) {
                    String tableNumber = (String) tbl.get("tableNumber");
                    incomingTableNums.add(tableNumber);
                    
                    DiningTable table = existingTables.stream()
                        .filter(t -> t.getTableNumber().equals(tableNumber))
                        .findFirst()
                        .orElse(null);
                        
                    if (table == null) {
                        table = new DiningTable();
                        table.setRestaurantId(restaurantId);
                        table.setTableNumber(tableNumber);
                        table.setStatus(TableStatus.AVAILABLE);
                    }
                    table.setCapacity((Integer) tbl.get("capacity"));
                    table.setAreaName((String) tbl.get("areaName"));
                    tableRepository.save(table);
                }
                
                // Delete tables that are no longer in the web dashboard
                for (DiningTable ext : existingTables) {
                    if (!incomingTableNums.contains(ext.getTableNumber())) {
                        try {
                            tableRepository.delete(ext);
                        } catch (Exception ignored) {}
                    }
                }
            }

            // 2. Sync Categories
            List<String> catList = (List<String>) config.get("categories");
            Map<String, Category> categoryMap = new HashMap<>();
            if (catList != null) {
                List<Category> existingCats = categoryRepository.findByRestaurantId(restaurantId);
                for (String catName : catList) {
                    Category cat = existingCats.stream()
                        .filter(c -> c.getName().equals(catName))
                        .findFirst()
                        .orElse(null);
                    if (cat == null) {
                        cat = new Category();
                        cat.setRestaurantId(restaurantId);
                        cat.setName(catName);
                        cat = categoryRepository.save(cat);
                    }
                    categoryMap.put(catName, cat);
                }
            }

            // 3. Sync Menu Items
            List<Map<String, Object>> itemsList = (List<Map<String, Object>>) config.get("menuItems");
            if (itemsList != null) {
                List<MenuItem> existingItems = menuRepository.findByRestaurantId(restaurantId);
                Set<String> incomingCodes = new HashSet<>();
                Set<String> incomingNames = new HashSet<>();
                
                for (Map<String, Object> itm : itemsList) {
                    String name = (String) itm.get("name");
                    String code = (String) itm.get("shortCode");
                    if (code == null || code.trim().isEmpty()) {
                        code = (name != null && name.length() >= 3) ? name.substring(0, 3).toUpperCase() : "ITM" + (int)(Math.random() * 1000);
                    }
                    final String finalCode = code.trim();
                    final String finalName = name != null ? name.trim() : "";

                    if (!finalCode.isEmpty()) incomingCodes.add(finalCode);
                    if (!finalName.isEmpty()) incomingNames.add(finalName.toLowerCase());
                    
                    MenuItem item = existingItems.stream()
                        .filter(i -> (i.getShortCode() != null && i.getShortCode().equalsIgnoreCase(finalCode)) ||
                                     (i.getName() != null && i.getName().equalsIgnoreCase(finalName)))
                        .findFirst()
                        .orElse(null);
                        
                    if (item == null) {
                        item = new MenuItem();
                        item.setRestaurantId(restaurantId);
                        item.setShortCode(finalCode);
                        item.setAvailable(true);
                    }
                    item.setName(finalName);
                    item.setShortCode(finalCode);
                    if (itm.get("price") != null) {
                        item.setPrice(new BigDecimal(itm.get("price").toString()));
                    }
                    if (itm.get("veg") != null) {
                        item.setVeg(Boolean.parseBoolean(itm.get("veg").toString()));
                    }
                    
                    String catName = (String) itm.get("categoryName");
                    Category category = categoryMap.get(catName);
                    item.setCategory(category);
                    item.setCategoryName(catName);
                    item.setDeleted(false);
                    menuRepository.save(item);
                }
                
                // Soft-delete items no longer present in incoming payload
                for (MenuItem ext : existingItems) {
                    boolean codeIn = ext.getShortCode() != null && incomingCodes.contains(ext.getShortCode());
                    boolean nameIn = ext.getName() != null && incomingNames.contains(ext.getName().toLowerCase());
                    if (!codeIn && !nameIn) {
                        ext.setDeleted(true);
                        menuRepository.save(ext);
                    }
                }
            }

            // 4. Sync Waiters
            List<Map<String, Object>> waitersList = (List<Map<String, Object>>) config.get("waiters");
            if (waitersList != null) {
                List<AppUser> existingUsers = userRepository.findAll().stream()
                    .filter(u -> restaurantId.equals(u.getRestaurantId()) && u.getRole() == UserRole.WAITER)
                    .toList();
                Set<String> incomingPins = new HashSet<>();

                for (Map<String, Object> w : waitersList) {
                    String pin = (String) w.get("pin");
                    if (pin == null || pin.trim().isEmpty()) continue;
                    String targetPin = pin.trim();
                    incomingPins.add(targetPin);

                    String targetUsername = "waiter_" + targetPin;
                    AppUser waiter = userRepository.findByUsername(targetUsername)
                        .orElseGet(() -> existingUsers.stream()
                            .filter(u -> targetPin.equals(u.getPin()))
                            .findFirst()
                            .orElse(null));

                    if (waiter == null) {
                        waiter = new AppUser();
                        waiter.setRestaurantId(restaurantId);
                        waiter.setRole(UserRole.WAITER);
                        waiter.setPassword(passwordEncoder.encode("waiter123"));
                    }
                    waiter.setPin(targetPin);
                    waiter.setUsername(targetUsername);
                    waiter.setFullName((String) w.get("name"));
                    String status = (String) w.get("status");
                    waiter.setActive(status == null || status.equalsIgnoreCase("Active"));
                    userRepository.save(waiter);
                }

                // Delete waiters that are no longer in the web dashboard
                for (AppUser ext : existingUsers) {
                    if (!incomingPins.contains(ext.getPin())) {
                        userRepository.delete(ext);
                    }
                }
            }
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Finishes local setup by registering the Manager admin user.
     */
    @Transactional
    public void setupManagerAccount(String username, String password, String pin) {
        SystemConfig config = systemConfigRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("System must be activated before setting up manager account"));

        TenantContext.setRestaurantId(config.getRestaurantId());
        try {
            // Save admin user
            AppUser admin = new AppUser();
            admin.setRestaurantId(config.getRestaurantId());
            admin.setUsername(username.trim());
            admin.setPassword(passwordEncoder.encode(password));
            admin.setRole(UserRole.ADMIN);
            admin.setFullName("SaaS Restaurant Owner");
            admin.setPin(pin.trim());
            admin.setActive(true);
            userRepository.save(admin);

            // Trigger mDNS broadcast service
            mdnsService.registerService(config.getRestaurantId());

        } finally {
            TenantContext.clear();
        }
    }
}

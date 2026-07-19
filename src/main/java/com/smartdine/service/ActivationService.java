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
    public void activateSystem(String activationCode, String gatewayUrl) throws Exception {
        if (activationCode == null || activationCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Activation code cannot be empty");
        }
        if (gatewayUrl == null || gatewayUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Cloud Gateway URL cannot be empty");
        }

        // 1. Fetch Configuration from Mock Cloud Gateway
        String url = gatewayUrl.trim() + "/activate?code=" + activationCode.trim();
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
        
        BigDecimal cgstRate = new BigDecimal(config.get("cgstRate").toString());
        BigDecimal sgstRate = new BigDecimal(config.get("sgstRate").toString());
        BigDecimal serviceChargeRate = new BigDecimal(config.get("serviceChargeRate").toString());

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

            // 4b. Upsert the Restaurant record so sync-code lookups work correctly.
            // Without this row, ProvisioningController cannot resolve the restaurant
            // by syncCode and falls back to hardcoded dummy waiters.
            com.smartdine.coreheart.Restaurant restaurantRecord =
                restaurantRepository.findBySyncCodeAndIsDeletedFalse(activationCode.trim())
                    .orElse(new com.smartdine.coreheart.Restaurant());
            restaurantRecord.setName(restaurantName);
            restaurantRecord.setSyncCode(activationCode.trim());
            restaurantRecord.setActive(true);
            // Restaurant entity uses restaurant_id to store its own UUID (it's the root).
            // Must satisfy the BaseEntity NOT NULL constraint.
            if (restaurantRecord.getRestaurantId() == null) {
                restaurantRecord.setRestaurantId(restaurantId);
            }
            restaurantRepository.save(restaurantRecord);
            System.out.println("✅ [ActivationService] Restaurant row upserted with syncCode=" + activationCode.trim());

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
                    item.setPrice(new BigDecimal(itm.get("price").toString()));
                    item.setVeg((Boolean) itm.get("veg"));
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
                    if (existingPins.contains(pin.trim())) {
                        // Waiter with this PIN was already added via admin panel — skip seeding
                        continue;
                    }
                    AppUser customWaiter = new AppUser();
                    customWaiter.setRestaurantId(restaurantId);
                    customWaiter.setUsername("waiter_" + pin.trim());
                    customWaiter.setPassword(passwordEncoder.encode("waiter123"));
                    customWaiter.setRole(UserRole.WAITER);
                    customWaiter.setFullName((String) w.get("name"));
                    customWaiter.setPin(pin.trim());
                    String status = (String) w.get("status");
                    customWaiter.setActive(status == null || status.equalsIgnoreCase("Active"));
                    userRepository.save(customWaiter);
                }
            }
            // NOTE: No fallback dummy waiter seeded — admin adds real waiters via the web panel.

            // Kitchen staff
            AppUser kitchen = new AppUser();
            kitchen.setRestaurantId(restaurantId);
            kitchen.setUsername("kitchen");
            kitchen.setPassword(passwordEncoder.encode("kitchen123"));
            kitchen.setRole(UserRole.KITCHEN);
            kitchen.setFullName("Main Kitchen");
            kitchen.setPin("5050");
            kitchen.setActive(true);
            userRepository.save(kitchen);

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
                List<MenuItem> existingItems = menuRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId);
                Set<String> incomingCodes = new HashSet<>();
                
                for (Map<String, Object> itm : itemsList) {
                    String code = (String) itm.get("shortCode");
                    incomingCodes.add(code);
                    
                    MenuItem item = existingItems.stream()
                        .filter(i -> code.equals(i.getShortCode()))
                        .findFirst()
                        .orElse(null);
                        
                    if (item == null) {
                        item = new MenuItem();
                        item.setRestaurantId(restaurantId);
                        item.setShortCode(code);
                        item.setAvailable(true);
                    }
                    item.setName((String) itm.get("name"));
                    item.setPrice(new BigDecimal(itm.get("price").toString()));
                    item.setVeg((Boolean) itm.get("veg"));
                    
                    String catName = (String) itm.get("categoryName");
                    Category category = categoryMap.get(catName);
                    item.setCategory(category);
                    item.setCategoryName(catName);
                    menuRepository.save(item);
                }
                
                // Mark items as deleted if they were removed from the web dashboard
                for (MenuItem ext : existingItems) {
                    if (!incomingCodes.contains(ext.getShortCode())) {
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
                    incomingPins.add(pin.trim());

                    AppUser waiter = existingUsers.stream()
                        .filter(u -> pin.trim().equals(u.getPin()))
                        .findFirst()
                        .orElse(null);

                    if (waiter == null) {
                        waiter = new AppUser();
                        waiter.setRestaurantId(restaurantId);
                        waiter.setPin(pin.trim());
                        waiter.setRole(UserRole.WAITER);
                        waiter.setPassword(passwordEncoder.encode("waiter123"));
                    }
                    waiter.setFullName((String) w.get("name"));
                    waiter.setUsername("waiter_" + pin.trim());
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

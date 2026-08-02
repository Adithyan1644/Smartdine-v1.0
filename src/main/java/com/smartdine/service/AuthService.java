package com.smartdine.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.smartdine.coreheart.*;
import com.smartdine.dto.AuthResponse;
import com.smartdine.dto.LoginRequest;
import com.smartdine.dto.PinLoginRequest;
import com.smartdine.repository.*;
import java.util.UUID;
import java.util.Map;
import java.util.Random;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CloudDatabaseSeederService cloudDatabaseSeederService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // Professional Multi-Credential Login for Admin/Setup (User ID, Phone Number, Restaurant Name, Email, Sync Code)
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public AuthResponse authenticateUser(LoginRequest request) {
        if (request == null || request.getUsername() == null) {
            throw new RuntimeException("User ID, Phone Number, Restaurant Name, or Sync Code is required");
        }

        String targetCred = request.getUsername().trim();
        String rawPassword = request.getPassword() != null ? request.getPassword().trim() : "";

        // 1. Try lookup by exact username / email
        AppUser user = userRepository.findByUsernameIgnoreCase(targetCred).orElse(null);

        // 2. Try lookup by Phone Number
        if (user == null) {
            user = userRepository.findAll().stream()
                    .filter(u -> u.getPhone() != null && u.getPhone().trim().equalsIgnoreCase(targetCred))
                    .findFirst().orElse(null);
        }

        // 3. Try lookup by Restaurant Name
        if (user == null) {
            var restOpt = restaurantRepository.findByNameIgnoreCase(targetCred);
            if (restOpt.isPresent()) {
                UUID restUuid = restOpt.get().getRestaurantId() != null ? restOpt.get().getRestaurantId() : restOpt.get().getId();
                user = userRepository.findByRestaurantIdAndRole(restUuid, UserRole.ADMIN)
                        .stream().findFirst()
                        .orElseGet(() -> userRepository.findByRestaurantId(restUuid).stream().findFirst().orElse(null));
            }
        }

        // 4. Try lookup by Sync Code (e.g. SD-620495 or 620495)
        if (user == null) {
            String candidateCode = targetCred.toUpperCase();
            if (!candidateCode.startsWith("SD-") && candidateCode.matches("\\d{6}")) {
                candidateCode = "SD-" + candidateCode;
            }
            String finalCode = candidateCode;
            var restOpt = restaurantRepository.findBySyncCodeAndIsDeletedFalse(finalCode)
                    .or(() -> restaurantRepository.findByBillerSyncCode(finalCode));
            if (restOpt.isPresent()) {
                UUID restUuid = restOpt.get().getRestaurantId() != null ? restOpt.get().getRestaurantId() : restOpt.get().getId();
                user = userRepository.findByRestaurantIdAndRole(restUuid, UserRole.ADMIN)
                        .stream().findFirst()
                        .orElseGet(() -> userRepository.findByRestaurantId(restUuid).stream().findFirst().orElse(null));
            }
        }

        // 5. Fallback search across all users
        if (user == null) {
            user = userRepository.findAll().stream()
                    .filter(u -> (u.getUsername() != null && u.getUsername().trim().equalsIgnoreCase(targetCred)) ||
                                 (u.getPhone() != null && u.getPhone().trim().equalsIgnoreCase(targetCred)))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Account not found. Please check your User ID, Phone Number, Restaurant Name, or Sync Code."));
        }

        boolean matchesBcrypt = user.getPassword() != null && passwordEncoder.matches(rawPassword, user.getPassword());
        boolean matchesPlaintext = user.getPassword() != null && rawPassword.equals(user.getPassword().trim());

        if (matchesBcrypt || matchesPlaintext) {
            if (matchesPlaintext && !matchesBcrypt) {
                user.setPassword(passwordEncoder.encode(rawPassword));
                userRepository.save(user);
            }

            String restName = "";
            String syncCode = "";
            if (user.getRestaurantId() != null) {
                final UUID uRestId = user.getRestaurantId();
                var r = restaurantRepository.findByRestaurantId(uRestId)
                        .or(() -> restaurantRepository.findById(uRestId))
                        .orElse(null);
                if (r != null) {
                    restName = r.getName();
                    syncCode = r.getBillerSyncCode() != null ? r.getBillerSyncCode() : r.getSyncCode();
                }
            }

            String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), user.getRestaurantId());
            AuthResponse response = new AuthResponse(token, user.getRole().name(), user.getRestaurantId(), user.getFullName());
            response.setRestaurantName(restName);
            response.setSyncCode(syncCode);
            return response;
        } else {
            throw new RuntimeException("Incorrect Password. Please try again.");
        }
    }

    // High-Speed PIN Login for Staff (Waiter/Kitchen/Biller)
    public AuthResponse authenticateWithPin(PinLoginRequest request) {
        java.util.List<AppUser> users = userRepository.findByPinAndRestaurantId(request.getPin(), request.getRestaurantId());
        java.util.Optional<AppUser> userOpt = users.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(users.get(0));
        if (!userOpt.isPresent()) {
            userOpt = userRepository.findAll().stream()
                    .filter(u -> request.getPin() != null && request.getPin().equals(u.getPin()))
                    .findFirst();
            
            // Dynamically copy/link the waiter under the requested restaurant ID so they belong to it
            if (userOpt.isPresent()) {
                AppUser original = userOpt.get();
                if (!original.getRestaurantId().equals(request.getRestaurantId())) {
                    String newUsername = original.getUsername() + "_" + request.getRestaurantId().toString().substring(0, 8);
                    java.util.Optional<AppUser> existing = userRepository.findByUsername(newUsername);
                    if (existing.isPresent()) {
                        userOpt = existing;
                    } else {
                        AppUser copy = new AppUser();
                        copy.setRestaurantId(request.getRestaurantId());
                        copy.setPin(original.getPin());
                        copy.setUsername(newUsername);
                        copy.setFullName(original.getFullName());
                        copy.setRole(original.getRole());
                        copy.setPassword(original.getPassword());
                        copy.setActive(true);
                        userRepository.save(copy);
                        userOpt = java.util.Optional.of(copy);
                        System.out.println("✅ AuthService: Dynamically provisioned waiter " + original.getFullName() + " for new restaurant ID: " + request.getRestaurantId());
                    }
                }
            }
        }
        AppUser user = userOpt.orElseThrow(() -> new RuntimeException("Invalid PIN"));

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), request.getRestaurantId());
        return new AuthResponse(token, user.getRole().name(), request.getRestaurantId(), user.getFullName());
    }

    public java.util.List<AppUser> getActiveWaiters(java.util.UUID restaurantId) {
        return getWaiters(restaurantId, true);
    }

    public java.util.List<AppUser> getWaiters(java.util.UUID restaurantId, boolean activeOnly) {
        java.util.List<AppUser> list = new java.util.ArrayList<>();
        if (restaurantId != null) {
            if (activeOnly) {
                list = userRepository.findByRestaurantIdAndRoleAndIsActiveTrue(restaurantId, com.smartdine.coreheart.UserRole.WAITER);
            } else {
                list = userRepository.findByRestaurantIdAndRole(restaurantId, com.smartdine.coreheart.UserRole.WAITER);
            }
        }
        
        if (list.isEmpty()) {
            list = userRepository.findAll().stream()
                .filter(u -> u.getRole() == com.smartdine.coreheart.UserRole.WAITER)
                .filter(u -> !activeOnly || u.isActive())
                .toList();
        }

        java.util.List<AppUser> unique = new java.util.ArrayList<>();
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (AppUser user : list) {
            String key = (user.getPin() != null && !user.getPin().trim().isEmpty()) 
                ? user.getPin().trim() 
                : (user.getUsername() != null ? user.getUsername().trim() : user.getId().toString());
            if (!keys.contains(key)) {
                keys.add(key);
                unique.add(user);
            }
        }
        return unique;
    }

    // Register a new waiter from the Admin Panel
    public AppUser registerWaiter(String fullName, String username, String pin, java.util.UUID restaurantId) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username '" + username + "' is already taken.");
        }
        if (pin == null || !pin.matches("\\d{4}")) {
            throw new RuntimeException("PIN must be exactly 4 digits.");
        }
        AppUser waiter = new AppUser();
        waiter.setFullName(fullName);
        waiter.setUsername(username);
        waiter.setPassword(passwordEncoder.encode(pin)); // Hash PIN as password too for compatibility
        waiter.setPin(pin);
        waiter.setRole(com.smartdine.coreheart.UserRole.WAITER);
        waiter.setRestaurantId(restaurantId);
        waiter.setActive(true);
        return userRepository.save(waiter);
    }

    // Activate or deactivate a waiter account
    public void setWaiterActive(java.util.UUID waiterId, boolean active) {
        AppUser waiter = userRepository.findById(waiterId)
                .orElseThrow(() -> new RuntimeException("Waiter not found with id: " + waiterId));
        waiter.setActive(active);
        userRepository.save(waiter);
    }

    @org.springframework.transaction.annotation.Transactional
    public java.util.Map<String, Object> registerNewTenant(com.smartdine.dto.OnboardingRequest request) {
        if (request == null || request.getRestaurantName() == null) {
            throw new RuntimeException("Restaurant name is required");
        }

        String restaurantName = request.getRestaurantName().trim();
        String ownerName = request.getOwnerName() != null ? request.getOwnerName().trim() : "SaaS Restaurant Owner";
        String loginUsername = request.getEmail() != null && !request.getEmail().trim().isEmpty()
                ? request.getEmail().trim()
                : (request.getPhone() != null && !request.getPhone().trim().isEmpty() ? request.getPhone().trim() : restaurantName.toLowerCase().replace(" ", ""));
        String rawPassword = request.getPassword() != null ? request.getPassword() : "123456";
        boolean isTest = request.isTest();

        String finalSyncCode = "";
        String waiterSyncCode = "";
        java.util.Random random = new java.util.Random();
        boolean unique = false;
        while (!unique) {
            int codeInt = 100000 + random.nextInt(900000);
            String candidate = "SD-" + codeInt;
            if (!restaurantRepository.findBySyncCodeAndIsDeletedFalse(candidate).isPresent()) {
                finalSyncCode = candidate;
                waiterSyncCode = "WT-" + codeInt;
                unique = true;
            }
        }

        UUID newRestId = UUID.randomUUID();
        Restaurant restaurant = new Restaurant(restaurantName, finalSyncCode, true);
        restaurant.setRestaurantId(newRestId);
        restaurant.setBillerSyncCode(finalSyncCode);
        restaurant.setWaiterSyncCode(waiterSyncCode);
        restaurant.setTest(isTest);
        restaurant = restaurantRepository.save(restaurant);

        AppUser admin = new AppUser();
        admin.setRestaurantId(newRestId);
        admin.setUsername(loginUsername);
        admin.setPhone(request.getPhone() != null ? request.getPhone().trim() : "");
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setRole(UserRole.ADMIN);
        admin.setFullName(ownerName);
        admin.setPin("1234");
        admin.setActive(true);
        userRepository.save(admin);

        // Save real-world setup data if provided in OnboardingRequest
        if (request.getAreas() != null && !request.getAreas().isEmpty()) {
            for (String areaName : request.getAreas()) {
                if (areaName != null && !areaName.trim().isEmpty()) {
                    try {
                        jdbcTemplate.update("INSERT INTO areas (id, name, restaurant_id) VALUES (?, ?, ?)", UUID.randomUUID(), areaName.trim(), newRestId);
                    } catch (Exception e) {
                        System.err.println("Area table insert notice: " + e.getMessage());
                    }
                }
            }
        }

        if (request.getTables() != null && !request.getTables().isEmpty()) {
            for (java.util.Map<String, Object> tableMap : request.getTables()) {
                DiningTable table = new DiningTable();
                table.setRestaurantId(newRestId);
                String tNum = (String) (tableMap.get("number") != null ? tableMap.get("number") : tableMap.get("tableName"));
                table.setTableNumber(tNum != null ? tNum : "T-01");
                Object cap = tableMap.get("capacity");
                table.setCapacity(cap != null ? Integer.parseInt(cap.toString()) : 4);
                table.setAreaName((String) (tableMap.get("area") != null ? tableMap.get("area") : tableMap.get("areaName")));
                table.setStatus(TableStatus.AVAILABLE);
                tableRepository.save(table);
            }
        }

        if (request.getMenuCategories() != null && !request.getMenuCategories().isEmpty()) {
            for (String catName : request.getMenuCategories()) {
                Category cat = new Category();
                cat.setRestaurantId(newRestId);
                cat.setName(catName);
                categoryRepository.save(cat);
            }
        }

        if (request.getMenuItems() != null && !request.getMenuItems().isEmpty()) {
            for (java.util.Map<String, Object> itemMap : request.getMenuItems()) {
                MenuItem item = new MenuItem();
                item.setRestaurantId(newRestId);
                item.setName((String) itemMap.get("name"));
                item.setShortCode((String) (itemMap.get("shortCode") != null ? itemMap.get("shortCode") : itemMap.get("code")));
                item.setCategoryName((String) (itemMap.get("categoryName") != null ? itemMap.get("categoryName") : itemMap.get("category")));
                Object priceObj = itemMap.get("price");
                item.setPrice(priceObj != null ? new java.math.BigDecimal(priceObj.toString()) : java.math.BigDecimal.ZERO);
                Object vegObj = itemMap.get("veg") != null ? itemMap.get("veg") : itemMap.get("isVeg");
                item.setVeg(vegObj != null ? Boolean.parseBoolean(vegObj.toString()) : true);
                item.setAvailable(true);
                menuRepository.save(item);
            }
        }

        // Clean-slate multi-environment architecture: zero dummy data seeding
        // Custom operational data is saved directly above when provided by merchant.

        return java.util.Map.of(
            "success", true,
            "syncCode", finalSyncCode,
            "restaurantId", newRestId.toString()
        );
    }

    @org.springframework.transaction.annotation.Transactional
    public java.util.Map<String, Object> registerNewTenant(String restaurantName, String username, String email, String password, boolean isTest) {
        // 1. Create and save a new Restaurant
        String finalSyncCode = "";
        String waiterSyncCode = "";
        java.util.Random random = new java.util.Random();
        boolean unique = false;
        while (!unique) {
            int codeInt = 100000 + random.nextInt(900000);
            String candidate = "SD-" + codeInt;
            if (!restaurantRepository.findBySyncCodeAndIsDeletedFalse(candidate).isPresent()) {
                finalSyncCode = candidate;
                waiterSyncCode = "WT-" + codeInt;
                unique = true;
            }
        }

        Restaurant restaurant = new Restaurant(restaurantName, finalSyncCode, true);
        restaurant.setBillerSyncCode(finalSyncCode);
        restaurant.setWaiterSyncCode(waiterSyncCode);
        restaurant.setRestaurantId(UUID.randomUUID()); // unique identifier for the tenant
        restaurant.setTest(isTest);                    // classify as DEV test or PROD live account
        restaurant = restaurantRepository.save(restaurant);


        UUID newRestId = restaurant.getRestaurantId();

        // 2. Create and save the Admin AppUser
        AppUser admin = new AppUser();
        admin.setRestaurantId(newRestId);
        admin.setUsername(username.trim());
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(UserRole.ADMIN);
        admin.setFullName("SaaS Restaurant Owner");
        admin.setPin("1234"); // default helper pin
        admin.setActive(true);
        userRepository.save(admin);

        // 3. Auto-Seed Complete Baseline Configuration (Tables, Categories, Menu Items, Waiters)
        cloudDatabaseSeederService.seedDefaultRestaurantData(newRestId);

        return java.util.Map.of(
            "syncCode", finalSyncCode,
            "restaurantId", newRestId.toString()
        );
    }
}
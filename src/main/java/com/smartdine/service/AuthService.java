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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // Professional Login for Admin/Setup
    public AuthResponse authenticateUser(LoginRequest request) {
        if (request == null || request.getUsername() == null) {
            throw new RuntimeException("Invalid Username");
        }

        String targetUsername = request.getUsername().trim();
        String rawPassword = request.getPassword() != null ? request.getPassword().trim() : "";

        AppUser user = userRepository.findByUsernameIgnoreCase(targetUsername)
                .orElseGet(() -> userRepository.findAll().stream()
                        .filter(u -> u.getUsername() != null && u.getUsername().trim().equalsIgnoreCase(targetUsername))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Invalid Username")));
        
        boolean matchesBcrypt = user.getPassword() != null && passwordEncoder.matches(rawPassword, user.getPassword());
        boolean matchesPlaintext = user.getPassword() != null && rawPassword.equals(user.getPassword().trim());

        if (matchesBcrypt || matchesPlaintext) {
            if (matchesPlaintext && !matchesBcrypt) {
                user.setPassword(passwordEncoder.encode(rawPassword));
                userRepository.save(user);
            }

            String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), user.getRestaurantId());
            return new AuthResponse(token, user.getRole().name(), user.getRestaurantId(), user.getFullName());
        } else {
            throw new RuntimeException("Invalid Password");
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

        // 3. Auto-Provision 4 Default Tables (T-01 to T-04)
        for (int i = 1; i <= 4; i++) {
            DiningTable table = new DiningTable();
            table.setRestaurantId(newRestId);
            table.setTableNumber(String.format("T-%02d", i));
            table.setCapacity(4);
            table.setAreaName("AC Area");
            table.setStatus(TableStatus.AVAILABLE);
            tableRepository.save(table);
        }

        // 4. Auto-Provision 3 Default Categories (Starters, Main Course, Desserts)
        String[] defaults = {"Starters", "Main Course", "Desserts"};
        for (String catName : defaults) {
            Category cat = new Category();
            cat.setRestaurantId(newRestId);
            cat.setName(catName);
            categoryRepository.save(cat);
        }

        return java.util.Map.of(
            "syncCode", finalSyncCode,
            "restaurantId", newRestId.toString()
        );
    }
}
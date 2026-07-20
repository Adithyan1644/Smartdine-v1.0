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
        AppUser user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid Username"));
        
        System.out.println(">>> DB password hash: " + user.getPassword());
        System.out.println(">>> Matches result: " + passwordEncoder.matches(request.getPassword(), user.getPassword()));

        if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), user.getRestaurantId());
            return new AuthResponse(token, user.getRole().name(), user.getRestaurantId(), user.getFullName());
        } else {
            throw new RuntimeException("Invalid Password");
        }
    }

    // High-Speed PIN Login for Staff (Waiter/Kitchen/Biller)
    public AuthResponse authenticateWithPin(PinLoginRequest request) {
        AppUser user = userRepository.findByPinAndRestaurantId(request.getPin(), request.getRestaurantId())
                .orElseThrow(() -> new RuntimeException("Invalid PIN"));

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), user.getRestaurantId());
        return new AuthResponse(token, user.getRole().name(), user.getRestaurantId(), user.getFullName());
    }

    public java.util.List<AppUser> getActiveWaiters(java.util.UUID restaurantId) {
        return userRepository.findByRestaurantIdAndRoleAndIsActiveTrue(restaurantId, com.smartdine.coreheart.UserRole.WAITER);
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
    public java.util.Map<String, Object> registerNewTenant(String restaurantName, String username, String email, String password) {
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
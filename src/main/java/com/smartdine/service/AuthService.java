package com.smartdine.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.smartdine.coreheart.AppUser;
import com.smartdine.coreheart.JwtUtil;
import com.smartdine.dto.AuthResponse;
import com.smartdine.dto.LoginRequest;
import com.smartdine.dto.PinLoginRequest;
import com.smartdine.repository.UserRepository;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

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
}
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
}
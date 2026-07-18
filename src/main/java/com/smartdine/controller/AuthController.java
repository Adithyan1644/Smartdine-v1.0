package com.smartdine.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.smartdine.dto.AuthResponse;
import com.smartdine.dto.LoginRequest;
import com.smartdine.dto.PinLoginRequest;
import com.smartdine.service.AuthService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.authenticateUser(request));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        try {
            String restaurantName = request.get("restaurantName");
            String username       = request.get("ownerName") != null ? request.get("ownerName") : request.get("username");
            String email          = request.get("email");
            String password       = request.get("password");
            
            if (restaurantName == null || username == null || email == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
            }
            
            Map<String, Object> result = authService.registerNewTenant(restaurantName, username, email, password);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/pin-login")
    public ResponseEntity<AuthResponse> pinLogin(@RequestBody PinLoginRequest request) {
        return ResponseEntity.ok(authService.authenticateWithPin(request));
    }

    @GetMapping("/waiters")
    public ResponseEntity<java.util.List<com.smartdine.coreheart.AppUser>> getWaiters(@RequestParam UUID restaurantId) {
        return ResponseEntity.ok(authService.getActiveWaiters(restaurantId));
    }

    @PostMapping("/register-waiter")
    public ResponseEntity<?> registerWaiter(@RequestBody Map<String, String> request) {
        try {
            String fullName    = request.get("fullName");
            String username    = request.get("username");
            String pin         = request.get("pin");
            String restIdStr   = request.get("restaurantId");
            UUID restaurantId  = UUID.fromString(restIdStr != null ? restIdStr : "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
            com.smartdine.coreheart.AppUser waiter = authService.registerWaiter(fullName, username, pin, restaurantId);
            return ResponseEntity.ok(Map.of("success", true, "id", waiter.getId().toString(), "username", waiter.getUsername()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PatchMapping("/waiters/{id}/deactivate")
    public ResponseEntity<?> deactivateWaiter(@PathVariable UUID id) {
        try {
            authService.setWaiterActive(id, false);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PatchMapping("/waiters/{id}/activate")
    public ResponseEntity<?> activateWaiter(@PathVariable UUID id) {
        try {
            authService.setWaiterActive(id, true);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
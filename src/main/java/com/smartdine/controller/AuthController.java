package com.smartdine.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.smartdine.config.DataSourceContextHolder;
import com.smartdine.dto.AuthResponse;
import com.smartdine.dto.LoginRequest;
import com.smartdine.dto.PinLoginRequest;
import com.smartdine.dto.RegisterRequest;
import com.smartdine.service.AuthService;
import com.smartdine.repository.RestaurantRepository;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/auth", "/api/auth"})
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private com.smartdine.repository.SystemConfigRepository systemConfigRepository;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.authenticateUser(request));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody com.smartdine.dto.OnboardingRequest request) {
        // ── Cloud SQL Routing ────────────────────────────────────────────────────
        // Set the datasource routing key BEFORE the @Transactional service call.
        String dsKey = request.isTest() ? DataSourceContextHolder.DEV
                                        : DataSourceContextHolder.PROD;
        DataSourceContextHolder.set(dsKey);
        System.out.println("[AuthController] Registration routing → " + dsKey
                + " (isTest=" + request.isTest() + ")");
        try {
            Map<String, Object> result = authService.registerNewTenant(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } finally {
            DataSourceContextHolder.clear();
        }
    }

    @PostMapping("/pin-login")
    public ResponseEntity<AuthResponse> pinLogin(@RequestBody PinLoginRequest request) {
        UUID activeId = systemConfigRepository.findAll().stream()
                .findFirst()
                .map(com.smartdine.coreheart.SystemConfig::getRestaurantId)
                .orElse(null);
        if (activeId != null) {
            request.setRestaurantId(activeId);
            System.out.println("🔐 AuthController: Scoped pinLogin to active tenant ID: " + activeId);
        }
        return ResponseEntity.ok(authService.authenticateWithPin(request));
    }

    /**
     * GET /auth/waiters
     *
     * Accepts EITHER:
     *   ?restaurantId=<uuid>   — direct UUID lookup (used by admin website)
     *   ?syncCode=SD-XXXXXX    — sync-code lookup (used by Waiter App)
     *
     * The syncCode path is the reliable one for the Waiter App because
     * it resolves the correct tenant regardless of which UUID was cached
     * locally on the device.
     */
    @GetMapping("/waiters")
    public ResponseEntity<?> getWaiters(
            @RequestParam(required = false) UUID restaurantId,
            @RequestParam(required = false) String syncCode,
            @RequestParam(required = false, defaultValue = "true") boolean activeOnly) {

        UUID resolvedId = null;

        // If the waiter app supplies a syncCode, resolve the restaurant UUID from it.
        // This is the canonical source of truth — eliminates UUID mismatch bugs.
        if (syncCode != null && !syncCode.trim().isEmpty()) {
            var restaurantOpt = restaurantRepository.findBySyncCodeAndIsDeletedFalse(syncCode.trim());
            if (restaurantOpt.isPresent()) {
                resolvedId = restaurantOpt.get().getRestaurantId();
            }
        }

        if (resolvedId == null) {
            resolvedId = restaurantId;
        }

        if (resolvedId == null) {
            resolvedId = com.smartdine.coreheart.TenantContext.getRestaurantId();
        }

        return ResponseEntity.ok(authService.getWaiters(resolvedId, activeOnly));
    }

    @PostMapping("/register-waiter")
    public ResponseEntity<?> registerWaiter(@RequestBody Map<String, String> request) {
        try {
            String fullName    = request.get("fullName");
            String username    = request.get("username");
            String pin         = request.get("pin");
            String restIdStr   = request.get("restaurantId");
            String syncCode    = request.get("syncCode");

            UUID restaurantId = null;

            // Prefer resolving by syncCode if restaurantId is the hardcoded default
            if (syncCode != null && !syncCode.trim().isEmpty()) {
                var restaurantOpt = restaurantRepository.findBySyncCodeAndIsDeletedFalse(syncCode.trim());
                if (restaurantOpt.isPresent()) {
                    restaurantId = restaurantOpt.get().getRestaurantId();
                }
            }

            if (restaurantId == null) {
                restaurantId = UUID.fromString(restIdStr != null ? restIdStr : "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
            }

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
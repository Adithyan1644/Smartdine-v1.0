package com.smartdine.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.smartdine.service.AuthService;
import com.smartdine.repository.RestaurantRepository;
import com.smartdine.coreheart.TenantContext;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @GetMapping("/waiters")
    public ResponseEntity<?> getWaiters(
            @RequestParam(required = false) UUID restaurantId,
            @RequestParam(required = false) String syncCode) {

        UUID resolvedId = null;

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
            resolvedId = TenantContext.getRestaurantId();
        }

        return ResponseEntity.ok(authService.getActiveWaiters(resolvedId));
    }
}

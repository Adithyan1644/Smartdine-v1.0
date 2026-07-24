package com.smartdine.controller;

import com.smartdine.coreheart.RestaurantSettings;
import com.smartdine.coreheart.TenantContext;
import com.smartdine.repository.RestaurantSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SettingsApiController {

    @Autowired
    private RestaurantSettingsRepository restaurantSettingsRepository;

    @GetMapping({"/settings", "/waiter/settings"})
    public ResponseEntity<RestaurantSettings> getRestaurantSettings(
            @RequestParam(required = false) String restaurantId) {
        UUID rid = null;
        if (restaurantId != null && !restaurantId.trim().isEmpty()) {
            try {
                rid = UUID.fromString(restaurantId.trim());
            } catch (Exception ignored) {}
        }
        if (rid == null) {
            rid = TenantContext.getRestaurantId();
        }
        if (rid == null) {
            rid = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
        }

        final UUID finalRid = rid;
        RestaurantSettings settings = restaurantSettingsRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    RestaurantSettings ns = new RestaurantSettings(finalRid);
                    return restaurantSettingsRepository.saveAndFlush(ns);
                });

        return ResponseEntity.ok(settings);
    }
}

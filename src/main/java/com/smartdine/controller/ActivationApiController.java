package com.smartdine.controller;

import com.smartdine.service.ActivationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/activation")
public class ActivationApiController {

    @Autowired
    private ActivationService activationService;

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        boolean activated = activationService.isSystemActivated();
        return ResponseEntity.ok(Map.of(
            "activated", activated,
            "restaurantId", activationService.getSystemConfig().map(c -> c.getRestaurantId().toString()).orElse(""),
            "restaurantName", activationService.getSystemConfig().map(c -> c.getRestaurantName()).orElse("")
        ));
    }

    @PostMapping("/activate")
    public ResponseEntity<?> activate(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String gateway = request.get("gatewayUrl");
        if (gateway == null || gateway.isEmpty()) {
            gateway = "http://localhost:8080/api/mock-cloud";
        }
        try {
            activationService.activateSystem(code, gateway);
            return ResponseEntity.ok(Map.of("success", true, "message", "System activated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/setup-manager")
    public ResponseEntity<?> setupManager(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String pin = request.get("pin");
        try {
            activationService.setupManagerAccount(username, password, pin);
            return ResponseEntity.ok(Map.of("success", true, "message", "Manager credentials configured successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}

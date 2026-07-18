package com.smartdine.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Cloud-side Controller to receive Swiggy/Zomato webhook events.
 */
@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    @Autowired
    private TunnelWebSocketHandler tunnelHandler;

    @PostMapping("/{provider}")
    public ResponseEntity<?> receiveWebhook(
            @PathVariable String provider,
            @RequestBody String payload,
            @RequestParam UUID restaurantId) {
        
        System.out.println("📥 [WebhookController] Received webhook from: " + provider + " | Restaurant: " + restaurantId);

        // Forward the payload down the active tunnel session
        boolean forwarded = tunnelHandler.forwardWebhook(restaurantId, provider, payload);

        if (forwarded) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Webhook routed successfully to local POS"));
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("success", false, "error", "Local POS system is offline or unreachable"));
        }
    }
}

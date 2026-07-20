package com.smartdine.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/webhooks")
@CrossOrigin(origins = "*")
public class WebhookController {

    @Autowired
    private TunnelWebSocketHandler tunnelHandler;

    @PostMapping("/aggregator")
    public ResponseEntity<String> receiveWebhook(
            @RequestParam("restaurantId") UUID restaurantId,
            @RequestBody String orderJsonPayload) {
        
        boolean delivered = tunnelHandler.forwardWebhook(restaurantId, orderJsonPayload);
        
        if (delivered) {
            return ResponseEntity.ok("Order routed successfully");
        } else {
            return ResponseEntity.status(503).body("Restaurant POS is currently OFFLINE");
        }
    }

    @PostMapping("/{provider}")
    public ResponseEntity<String> receiveProviderWebhook(
            @PathVariable("provider") String provider,
            @RequestParam("restaurantId") UUID restaurantId,
            @RequestBody String orderJsonPayload) {
        
        boolean delivered = tunnelHandler.forwardWebhook(restaurantId, provider, orderJsonPayload);
        
        if (delivered) {
            return ResponseEntity.ok("Order routed successfully");
        } else {
            return ResponseEntity.status(503).body("Restaurant POS is currently OFFLINE");
        }
    }
}

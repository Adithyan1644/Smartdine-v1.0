package com.smartdine.controller;

import com.smartdine.coreheart.KOT;
import com.smartdine.coreheart.KOTItem;
import com.smartdine.coreheart.KOTStatus;
import com.smartdine.coreheart.TenantContext;
import com.smartdine.repository.KOTRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/kitchen/kot")
public class KitchenController {

    @Autowired
    private KOTRepository kotRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // 1. Fetch active KOTs (PENDING and PREPARING) for this restaurant
    @GetMapping("/active")
    public ResponseEntity<List<KOT>> getActiveKOTs() {
        UUID restaurantId = TenantContext.getRestaurantId();
        
        // Fetch tickets that are currently PENDING, PREPARING, or SERVED (not fully finished/ready in KDS)
        List<KOT> activeKOTs = kotRepository.findByRestaurantIdAndOverallStatusIn(
                restaurantId, 
                List.of(KOTStatus.PENDING, KOTStatus.PREPARING, KOTStatus.SERVED)
        );
        return ResponseEntity.ok(activeKOTs);
    }

    // 2. Chef updates status (PENDING -> PREPARING or PREPARING -> READY)
    @PutMapping("/{id}/status")
    public ResponseEntity<KOT> updateKOTStatus(
            @PathVariable("id") UUID id, 
            @RequestBody Map<String, String> body) {
        
        UUID restaurantId = TenantContext.getRestaurantId();
        KOT kot = kotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("KOT not found"));

        if (!kot.getRestaurantId().equals(restaurantId)) {
            throw new RuntimeException("Access denied");
        }

        String statusStr = body.get("status");
        KOTStatus newStatus = KOTStatus.valueOf(statusStr.toUpperCase());
        
        kot.setOverallStatus(newStatus);
        
        // Update all items inside the ticket to the new status as well
        for (KOTItem item : kot.getItems()) {
            item.setItemStatus(newStatus);
        }

        KOT updatedKot = kotRepository.save(kot);

        // Notify Waiters and KDS in real-time about the state change!
        messagingTemplate.convertAndSend("/topic/kitchen/" + restaurantId, updatedKot);

        return ResponseEntity.ok(updatedKot);
    }

    // 3. Chef updates status of a single item in a KOT
    @PutMapping("/{kotId}/item/{itemId}/status")
    public ResponseEntity<?> updateKOTItemStatus(
            @PathVariable("kotId") UUID kotId,
            @PathVariable("itemId") UUID itemId,
            @RequestBody Map<String, String> body) {
        try {
            UUID restaurantId = TenantContext.getRestaurantId();
            System.out.println("[KDS DEBUG] updateKOTItemStatus: kotId=" + kotId + ", itemId=" + itemId + ", body=" + body);
            
            KOT kot = kotRepository.findById(kotId)
                    .orElseThrow(() -> new RuntimeException("KOT not found: " + kotId));

            if (!kot.getRestaurantId().equals(restaurantId)) {
                System.out.println("[KDS DEBUG] Access denied: kot.restaurantId=" + kot.getRestaurantId() + ", context.restaurantId=" + restaurantId);
                throw new RuntimeException("Access denied");
            }

            String statusStr = body.get("status");
            if (statusStr == null) {
                throw new IllegalArgumentException("Status value is missing in request body");
            }
            KOTStatus itemStatus = KOTStatus.valueOf(statusStr.toUpperCase());

            boolean itemFound = false;
            for (KOTItem item : kot.getItems()) {
                System.out.println("[KDS DEBUG] checking item ID: " + item.getId() + " against requested itemId: " + itemId);
                if (item.getId().equals(itemId)) {
                    item.setItemStatus(itemStatus);
                    itemFound = true;
                    break;
                }
            }

            if (!itemFound) {
                System.out.println("[KDS DEBUG] Item not found in KOT: kotId=" + kotId + ", itemId=" + itemId);
                throw new RuntimeException("Item not found in KOT");
            }

            // Automatic state transition logic for KOT
            boolean anyPreparingOrReady = false;
            for (KOTItem item : kot.getItems()) {
                if (item.getItemStatus() == KOTStatus.PREPARING || item.getItemStatus() == KOTStatus.READY) {
                    anyPreparingOrReady = true;
                    break;
                }
            }
            
            if (anyPreparingOrReady && kot.getOverallStatus() == KOTStatus.PENDING) {
                kot.setOverallStatus(KOTStatus.PREPARING);
            }

            KOT updatedKot = kotRepository.save(kot);

            // Notify Waiters and KDS in real-time about the state change!
            messagingTemplate.convertAndSend("/topic/kitchen/" + restaurantId, updatedKot);

            return ResponseEntity.ok(updatedKot);
        } catch (Exception e) {
            System.err.println("[KDS ERROR] Failed to update KOT item status: " + e.getMessage());
            e.printStackTrace();
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            String stackTrace = sw.toString();
            if (stackTrace.length() > 500) {
                stackTrace = stackTrace.substring(0, 500) + "...";
            }
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage() != null ? e.getMessage() : "Unknown error",
                "type", e.getClass().getName(),
                "stacktrace", stackTrace
            ));
        }
    }
}

package com.smartdine.controller;

import com.smartdine.coreheart.Order;
import com.smartdine.coreheart.OrderStatus;
import com.smartdine.coreheart.OrderType;
import com.smartdine.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Cloud API Controller receiving Direction A sales archiving requests from
 * local Billing PCs.
 * Writes transaction data directly into Google Cloud SQL Master Database.
 */
@RestController
@RequestMapping("/api/sync")
@CrossOrigin(origins = "*")
public class SyncApiController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private com.smartdine.repository.RestaurantRepository restaurantRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Receives and processes local transaction outbox payloads at /api/sync/process
     */
    @PostMapping("/process")
    public ResponseEntity<?> processSyncEvent(
            @RequestParam(name = "type", required = false, defaultValue = "ORDER") String eventType,
            @RequestParam(name = "code", required = false) String paramCode,
            @RequestHeader(name = "X-Sync-Code", required = false) String headerCode,
            @RequestBody(required = false) String payloadJson) {

        String effectiveCode = paramCode != null && !paramCode.trim().isEmpty() ? paramCode.trim() : headerCode;
        System.out.println("📶 [Cloud Gateway] Processing incoming sync event of type: " + eventType + " (code=" + effectiveCode + ")");

        if (payloadJson == null || payloadJson.trim().isEmpty()) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Heartbeat event acknowledged"));
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);
            UUID restaurantId = resolveRestaurantId(payload, effectiveCode);

            if ("ORDER".equalsIgnoreCase(eventType) || "ORDER_SETTLED".equalsIgnoreCase(eventType) || "ORDER_CREATED".equalsIgnoreCase(eventType)) {
                Order order = mapPayloadToOrder(payload, effectiveCode);
                if (restaurantId != null) {
                    order.setRestaurantId(restaurantId);
                }
                orderRepository.save(order);
                System.out.println("✅ Order [" + order.getOrderNumber() + "] committed to GCP Cloud SQL for restaurant: " + order.getRestaurantId());
            } else if ("MENU_UPDATE".equalsIgnoreCase(eventType) || "MENU".equalsIgnoreCase(eventType)) {
                System.out.println("Menu update event received on cloud for Restaurant ID: " + restaurantId);
            } else {
                System.out.println("Notice: Received sync event type: " + eventType);
            }

            return ResponseEntity.ok(Map.of("success", true, "message", "Event processed successfully"));

        } catch (Exception e) {
            System.err.println("Failed to process cloud sync payload: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Failed to process payload: " + e.getMessage()));
        }
    }

    @PostMapping("/orders")
    public ResponseEntity<?> syncSingleOrder(
            @RequestParam(name = "code", required = false) String paramCode,
            @RequestHeader(name = "X-Sync-Code", required = false) String headerCode,
            @RequestBody Map<String, Object> payload) {
        try {
            String effectiveCode = paramCode != null && !paramCode.trim().isEmpty() ? paramCode.trim() : headerCode;
            Order order = mapPayloadToOrder(payload, effectiveCode);
            orderRepository.save(order);
            System.out.println("☁️ GCP Cloud SQL: Successfully archived order " + order.getOrderNumber()
                    + " for Restaurant: " + order.getRestaurantId());
            return ResponseEntity.ok(Map.of("success", true, "message", "Order archived to Cloud SQL"));
        } catch (Exception e) {
            System.err.println("❌ Cloud Sync Error: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/orders/bulk")
    public ResponseEntity<?> syncBulkOrders(
            @RequestParam(name = "code", required = false) String paramCode,
            @RequestHeader(name = "X-Sync-Code", required = false) String headerCode,
            @RequestBody List<Map<String, Object>> payloads) {
        try {
            String effectiveCode = paramCode != null && !paramCode.trim().isEmpty() ? paramCode.trim() : headerCode;
            int count = 0;
            for (Map<String, Object> payload : payloads) {
                Order order = mapPayloadToOrder(payload, effectiveCode);
                orderRepository.save(order);
                count++;
            }
            System.out.println("🚀 GCP Cloud SQL: Bulk uploaded " + count + " offline sales to Cloud SQL database.");
            return ResponseEntity.ok(Map.of("success", true, "syncedCount", count));
        } catch (Exception e) {
            System.err.println("❌ Bulk Cloud Sync Error: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    private UUID resolveRestaurantId(Map<String, Object> payload, String fallbackCode) {
        if (payload != null && payload.get("restaurantId") != null && !payload.get("restaurantId").toString().trim().isEmpty()) {
            try {
                return UUID.fromString(payload.get("restaurantId").toString().trim());
            } catch (Exception ignored) {}
        }

        String codeToSearch = null;
        if (payload != null && payload.get("syncCode") != null) {
            codeToSearch = payload.get("syncCode").toString().trim();
        } else if (payload != null && payload.get("code") != null) {
            codeToSearch = payload.get("code").toString().trim();
        } else if (fallbackCode != null && !fallbackCode.trim().isEmpty()) {
            codeToSearch = fallbackCode.trim();
        }

        if (codeToSearch != null && !codeToSearch.isEmpty()) {
            var restOpt = restaurantRepository.findBySyncCodeAndIsDeletedFalse(codeToSearch);
            if (restOpt.isPresent()) {
                com.smartdine.coreheart.Restaurant r = restOpt.get();
                return r.getId() != null ? r.getId() : r.getRestaurantId();
            }
        }

        return com.smartdine.coreheart.TenantContext.getRestaurantId();
    }

    private Order mapPayloadToOrder(Map<String, Object> payload, String fallbackCode) {
        Order order = new Order();
        UUID restId = resolveRestaurantId(payload, fallbackCode);
        if (restId != null) {
            order.setRestaurantId(restId);
        }

        order.setOrderNumber(payload.get("orderNumber") != null ? payload.get("orderNumber").toString() : "#1000");

        if (payload.get("type") != null) {
            try {
                order.setType(OrderType.valueOf(payload.get("type").toString()));
            } catch (Exception e) {
                order.setType(OrderType.DINE_IN);
            }
        } else {
            order.setType(OrderType.DINE_IN);
        }

        order.setSource(payload.get("source") != null ? payload.get("source").toString() : "DIRECT");
        order.setStatus(OrderStatus.PAID);

        if (payload.get("subTotal") != null)
            order.setSubTotal(new BigDecimal(payload.get("subTotal").toString()));
        if (payload.get("cgst") != null)
            order.setCgst(new BigDecimal(payload.get("cgst").toString()));
        if (payload.get("sgst") != null)
            order.setSgst(new BigDecimal(payload.get("sgst").toString()));
        if (payload.get("discount") != null)
            order.setDiscount(new BigDecimal(payload.get("discount").toString()));
        if (payload.get("grandTotal") != null)
            order.setGrandTotal(new BigDecimal(payload.get("grandTotal").toString()));

        order.setPaymentMode(payload.get("paymentMode") != null ? payload.get("paymentMode").toString() : "CASH");
        order.setCustomerName(payload.get("customerName") != null ? payload.get("customerName").toString() : "Walk-in");
        order.setCustomerPhone(payload.get("customerPhone") != null ? payload.get("customerPhone").toString() : "");

        if (payload.get("startedAt") != null) {
            try {
                order.setStartedAt(LocalDateTime.parse(payload.get("startedAt").toString()));
            } catch (Exception ignored) {
            }
        }
        if (payload.get("settledAt") != null) {
            try {
                order.setSettledAt(LocalDateTime.parse(payload.get("settledAt").toString()));
            } catch (Exception ignored) {
                order.setSettledAt(LocalDateTime.now());
            }
        } else {
            order.setSettledAt(LocalDateTime.now());
        }

        return order;
    }
}

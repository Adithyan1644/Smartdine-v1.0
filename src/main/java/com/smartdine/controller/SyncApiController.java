package com.smartdine.controller;

import com.smartdine.coreheart.Order;
import com.smartdine.coreheart.OrderStatus;
import com.smartdine.coreheart.OrderType;
import com.smartdine.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Cloud API Controller receiving Direction A sales archiving requests from local Billing PCs.
 * Writes transaction data directly into Google Cloud SQL Master Database.
 */
@RestController
@RequestMapping("/api/sync")
@CrossOrigin(origins = "*")
public class SyncApiController {

    @Autowired
    private OrderRepository orderRepository;

    @PostMapping("/orders")
    public ResponseEntity<?> syncSingleOrder(@RequestBody Map<String, Object> payload) {
        try {
            Order order = mapPayloadToOrder(payload);
            orderRepository.save(order);
            System.out.println("☁️ GCP Cloud SQL: Successfully archived order " + order.getOrderNumber() + " for Restaurant: " + order.getRestaurantId());
            return ResponseEntity.ok(Map.of("success", true, "message", "Order archived to Cloud SQL"));
        } catch (Exception e) {
            System.err.println("❌ Cloud Sync Error: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/orders/bulk")
    public ResponseEntity<?> syncBulkOrders(@RequestBody List<Map<String, Object>> payloads) {
        try {
            int count = 0;
            for (Map<String, Object> payload : payloads) {
                Order order = mapPayloadToOrder(payload);
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

    private Order mapPayloadToOrder(Map<String, Object> payload) {
        Order order = new Order();

        if (payload.get("restaurantId") != null) {
            try {
                order.setRestaurantId(UUID.fromString(payload.get("restaurantId").toString()));
            } catch (Exception ignored) {}
        }
        if (order.getRestaurantId() == null) {
            order.setRestaurantId(UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"));
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

        if (payload.get("subTotal") != null) order.setSubTotal(new BigDecimal(payload.get("subTotal").toString()));
        if (payload.get("cgst") != null) order.setCgst(new BigDecimal(payload.get("cgst").toString()));
        if (payload.get("sgst") != null) order.setSgst(new BigDecimal(payload.get("sgst").toString()));
        if (payload.get("discount") != null) order.setDiscount(new BigDecimal(payload.get("discount").toString()));
        if (payload.get("grandTotal") != null) order.setGrandTotal(new BigDecimal(payload.get("grandTotal").toString()));

        order.setPaymentMode(payload.get("paymentMode") != null ? payload.get("paymentMode").toString() : "CASH");
        order.setCustomerName(payload.get("customerName") != null ? payload.get("customerName").toString() : "Walk-in");
        order.setCustomerPhone(payload.get("customerPhone") != null ? payload.get("customerPhone").toString() : "");

        if (payload.get("startedAt") != null) {
            try {
                order.setStartedAt(LocalDateTime.parse(payload.get("startedAt").toString()));
            } catch (Exception ignored) {}
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

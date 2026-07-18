package com.smartdine.controller;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
 
import com.smartdine.coreheart.DiningTable;
import com.smartdine.coreheart.TenantContext;
import com.smartdine.coreheart.Order;
import com.smartdine.coreheart.OrderStatus;
import com.smartdine.repository.TableRepository;
import com.smartdine.repository.OrderRepository;
import com.smartdine.repository.KOTRepository;
import com.smartdine.coreheart.KOT;
import org.springframework.http.ResponseEntity;

 
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
 
@RestController
@RequestMapping("/api/admin/tables")
public class TableController {
 
    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private KOTRepository kotRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
 
    // 1. Setup a New Table (e.g., ACT1)
    @PostMapping
    public DiningTable createTable(@RequestBody DiningTable table) {
        table.setRestaurantId(TenantContext.getRestaurantId());
        return tableRepository.save(table);
    }
 
    // 2. Get Live Table Status for the Biller Dashboard
    @GetMapping
    public List<DiningTable> getAllTables() {
        UUID restaurantId = TenantContext.getRestaurantId();
        List<DiningTable> tables = tableRepository.findByRestaurantIdOrderByTableNumberAsc(restaurantId);
        List<Order> activeOrders = orderRepository.findByRestaurantIdAndStatusNotIn(restaurantId, java.util.Arrays.asList(OrderStatus.PAID, OrderStatus.CANCELLED));

        for (DiningTable table : tables) {
            if (table.getStatus() != com.smartdine.coreheart.TableStatus.AVAILABLE) {
                Optional<Order> activeOrderOpt = activeOrders.stream()
                        .filter(o -> {
                            if (o.getTableId() != null && o.getTableId().equals(table.getId())) {
                                return true;
                            }
                            if (o.getMergedTableIds() != null && !o.getMergedTableIds().trim().isEmpty()) {
                                for (String id : o.getMergedTableIds().split(",")) {
                                    if (id.trim().equals(table.getId().toString())) {
                                        return true;
                                    }
                                }
                            }
                            return false;
                        })
                        .findFirst();
                if (activeOrderOpt.isPresent()) {
                    Order order = activeOrderOpt.get();
                    table.setTotalAmount(order.getGrandTotal() != null ? order.getGrandTotal().doubleValue() : 0.0);
                    if (order.getStartedAt() != null) {
                        table.setDurationMinutes((int) java.time.Duration.between(order.getStartedAt(), java.time.LocalDateTime.now()).toMinutes());
                    }
                    if (order.getMergedTableIds() != null && !order.getMergedTableIds().trim().isEmpty()) {
                        table.setMerged(true);
                        java.util.List<String> mIds = java.util.Arrays.asList(order.getMergedTableIds().split(","));
                        java.util.List<String> mNames = new java.util.ArrayList<>();
                        for (DiningTable t : tables) {
                            if (mIds.contains(t.getId().toString())) {
                                mNames.add(t.getTableNumber());
                            }
                        }
                        mNames.sort(String::compareTo);
                        table.setMergedTableNames(String.join(", ", mNames));
                    }
                }
            }
        }
        return tables;
    }

    // 3. Update Table Status (Settle/Bill/Clear)
    @PatchMapping("/{id}/status")
    public DiningTable updateTableStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        UUID restaurantId = TenantContext.getRestaurantId();
        DiningTable table = tableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Table not found"));
        
        if (!table.getRestaurantId().equals(restaurantId)) {
            throw new RuntimeException("Table does not belong to this restaurant");
        }

        String statusStr = body.get("status");
        if (statusStr != null) {
            com.smartdine.coreheart.TableStatus newStatus = com.smartdine.coreheart.TableStatus.valueOf(statusStr.toUpperCase());
            table.setStatus(newStatus);
            tableRepository.save(table);

            // Broadcast websocket update to /topic/tables/{restaurantId}
            String topic = "/topic/tables/" + restaurantId;

            // If updating status, let's also update the active order status
            List<Order> activeOrders = orderRepository.findByRestaurantIdAndStatusNotIn(restaurantId, java.util.Arrays.asList(OrderStatus.PAID, OrderStatus.CANCELLED));
            Optional<Order> activeOrderOpt = activeOrders.stream()
                    .filter(o -> {
                        if (o.getTableId() != null && o.getTableId().equals(table.getId())) {
                            return true;
                        }
                        if (o.getMergedTableIds() != null && !o.getMergedTableIds().trim().isEmpty()) {
                            for (String mId : o.getMergedTableIds().split(",")) {
                                if (mId.trim().equals(table.getId().toString())) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    })
                    .findFirst();

            if (activeOrderOpt.isPresent()) {
                Order order = activeOrderOpt.get();
                if (newStatus == com.smartdine.coreheart.TableStatus.AVAILABLE) {
                    order.setStatus(OrderStatus.PAID);
                    order.setSettledAt(java.time.LocalDateTime.now());
                    orderRepository.save(order);

                    // Cascade settlement to other merged tables
                    if (order.getMergedTableIds() != null && !order.getMergedTableIds().trim().isEmpty()) {
                        for (String idStr : order.getMergedTableIds().split(",")) {
                            try {
                                UUID otherId = UUID.fromString(idStr.trim());
                                if (!otherId.equals(table.getId())) {
                                    DiningTable otherTable = tableRepository.findById(otherId).orElse(null);
                                    if (otherTable != null) {
                                        otherTable.setStatus(com.smartdine.coreheart.TableStatus.AVAILABLE);
                                        tableRepository.save(otherTable);
                                        
                                        // Broadcast other table availability
                                        Map<String, Object> otherPayload = new HashMap<>();
                                        otherPayload.put("id", otherTable.getId().toString());
                                        otherPayload.put("status", com.smartdine.coreheart.TableStatus.AVAILABLE.name());
                                        otherPayload.put("totalAmount", 0.0);
                                        otherPayload.put("durationMinutes", 0);
                                        messagingTemplate.convertAndSend(topic, otherPayload);
                                    }
                                }
                            } catch (Exception ex) {
                                System.err.println("Error releasing merged table: " + ex.getMessage());
                            }
                        }
                    }
                } else if (newStatus == com.smartdine.coreheart.TableStatus.PAYMENT_PENDING) {
                    order.setStatus(OrderStatus.BILLED);
                    orderRepository.save(order);

                    // Cascade PAYMENT_PENDING to other merged tables
                    if (order.getMergedTableIds() != null && !order.getMergedTableIds().trim().isEmpty()) {
                        for (String idStr : order.getMergedTableIds().split(",")) {
                            try {
                                UUID otherId = UUID.fromString(idStr.trim());
                                if (!otherId.equals(table.getId())) {
                                    DiningTable otherTable = tableRepository.findById(otherId).orElse(null);
                                    if (otherTable != null) {
                                        otherTable.setStatus(com.smartdine.coreheart.TableStatus.PAYMENT_PENDING);
                                        tableRepository.save(otherTable);
                                        
                                        // Broadcast other table pending status
                                        Map<String, Object> otherPayload = new HashMap<>();
                                        otherPayload.put("id", otherTable.getId().toString());
                                        otherPayload.put("status", com.smartdine.coreheart.TableStatus.PAYMENT_PENDING.name());
                                        otherPayload.put("totalAmount", order.getGrandTotal() != null ? order.getGrandTotal().doubleValue() : 0.0);
                                        if (order.getStartedAt() != null) {
                                            otherPayload.put("durationMinutes", (int) java.time.Duration.between(order.getStartedAt(), java.time.LocalDateTime.now()).toMinutes());
                                        } else {
                                            otherPayload.put("durationMinutes", 0);
                                        }
                                        messagingTemplate.convertAndSend(topic, otherPayload);
                                    }
                                }
                            } catch (Exception ex) {
                                System.err.println("Error updating merged table status: " + ex.getMessage());
                            }
                        }
                    }
                } else if (newStatus == com.smartdine.coreheart.TableStatus.RUNNING) {
                    order.setStatus(OrderStatus.OPEN);
                    orderRepository.save(order);
                }
            }

            Map<String, Object> wsPayload = new HashMap<>();
            wsPayload.put("id", table.getId().toString());
            wsPayload.put("status", newStatus.name());
            
            double total = 0.0;
            int duration = 0;
            if (newStatus != com.smartdine.coreheart.TableStatus.AVAILABLE && activeOrderOpt.isPresent()) {
                Order order = activeOrderOpt.get();
                total = order.getGrandTotal() != null ? order.getGrandTotal().doubleValue() : 0.0;
                if (order.getStartedAt() != null) {
                    duration = (int) java.time.Duration.between(order.getStartedAt(), java.time.LocalDateTime.now()).toMinutes();
                }
            }
            wsPayload.put("totalAmount", total);
            wsPayload.put("durationMinutes", duration);

            messagingTemplate.convertAndSend(topic, wsPayload);
            System.out.println("📢 WebSocket Table Update Broadcast sent to topic: " + topic);
        }

        return table;
    }

    // 4. Get active KOTs and order items for a specific table
    @GetMapping("/{id}/kots")
    public ResponseEntity<List<KOT>> getTableKOTs(@PathVariable("id") UUID id) {
        UUID restaurantId = TenantContext.getRestaurantId();
        List<Order> activeOrders = orderRepository.findByRestaurantIdAndStatusNotIn(
            restaurantId, 
            java.util.Arrays.asList(OrderStatus.PAID, OrderStatus.CANCELLED)
        );

        Optional<Order> activeOrderOpt = activeOrders.stream()
                .filter(o -> {
                    if (o.getTableId() != null && o.getTableId().equals(id)) {
                        return true;
                    }
                    if (o.getMergedTableIds() != null && !o.getMergedTableIds().trim().isEmpty()) {
                        for (String mId : o.getMergedTableIds().split(",")) {
                            if (mId.trim().equals(id.toString())) {
                                return true;
                            }
                        }
                    }
                    return false;
                })
                .findFirst();

        if (activeOrderOpt.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        UUID orderId = activeOrderOpt.get().getId();
        List<KOT> kots = kotRepository.findByOrderId(orderId);
        return ResponseEntity.ok(kots);
    }
}
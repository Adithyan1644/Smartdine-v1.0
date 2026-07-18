package com.smartdine.service;

import com.smartdine.coreheart.*;
import com.smartdine.repository.*;
import com.smartdine.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate; // Added for WebSockets
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private KOTRepository kotRepository;

    @Autowired
    private ModifierOptionRepository modifierOptionRepository;

    @Autowired
    private ModifierGroupRepository modifierGroupRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate; // Injected the WebSocket Publisher

    @Transactional
    public KOT processNewKOT(OrderRequest request) {
        UUID restaurantId = TenantContext.getRestaurantId();

        // 1. Fetch & Verify the Table
        DiningTable table = tableRepository.findById(request.getTableId())
                .orElseThrow(() -> new RuntimeException("Table not found"));

        if (!table.getRestaurantId().equals(restaurantId)) {
            throw new RuntimeException("Table does not belong to this restaurant");
        }

        // 2. Find or Create the Active Order session for this Table
        Order order = orderRepository.findByRestaurantIdAndStatusNotIn(restaurantId, Arrays.asList(OrderStatus.PAID, OrderStatus.CANCELLED))
                .stream()
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
                .findFirst()
                .orElseGet(() -> createNewOrder(table, request.getNotes()));

        // 3. Create the KOT Ticket
        KOT kot = new KOT();
        kot.setRestaurantId(restaurantId);
        kot.setOrderId(order.getId());
        kot.setTableId(table.getId());
        kot.setTableName(order.getTableName() != null ? order.getTableName() : table.getTableNumber());
        kot.setKotNumber("KOT-" + (System.currentTimeMillis() % 100000));
        kot.setNotes(request.getNotes());

        BigDecimal kotSubtotal = BigDecimal.ZERO;
        List<KOTItem> kotItems = new ArrayList<>();

        // 4. Validate items and lock current database prices (protects against client tampering)
        for (KOTItemRequest itemReq : request.getItems()) {
            MenuItem menuItem = menuRepository.findById(itemReq.getMenuItemId())
                    .orElseThrow(() -> new RuntimeException("Menu item not found: " + itemReq.getMenuItemId()));

            if (!menuItem.getRestaurantId().equals(restaurantId)) {
                throw new RuntimeException("Menu item does not belong to this restaurant");
            }

            // Resolve modifiers
            BigDecimal itemSpecificModifiersPrice = BigDecimal.ZERO;
            BigDecimal globalModifiersPrice = BigDecimal.ZERO;
            List<String> modifierNames = new ArrayList<>();
            if (itemReq.getModifierOptionIds() != null && !itemReq.getModifierOptionIds().isEmpty()) {
                List<ModifierOption> uniqueOptions = modifierOptionRepository.findAllById(itemReq.getModifierOptionIds());
                Map<UUID, ModifierOption> optionMap = new HashMap<>();
                for (ModifierOption opt : uniqueOptions) {
                    optionMap.put(opt.getId(), opt);
                }

                for (UUID optId : itemReq.getModifierOptionIds()) {
                    ModifierOption option = optionMap.get(optId);
                    if (option != null && option.getRestaurantId().equals(restaurantId)) {
                        Boolean isGlobal = modifierGroupRepository.isOptionGlobal(optId);
                        if (isGlobal != null && isGlobal) {
                            globalModifiersPrice = globalModifiersPrice.add(option.getPrice());
                        } else {
                            itemSpecificModifiersPrice = itemSpecificModifiersPrice.add(option.getPrice());
                        }
                        modifierNames.add(option.getName());
                    }
                }
            }

            String finalInstruction = itemReq.getSpecialInstruction() != null ? itemReq.getSpecialInstruction() : "";
            if (!modifierNames.isEmpty()) {
                String modsString = String.join(", ", modifierNames);
                if (finalInstruction.isEmpty()) {
                    finalInstruction = modsString;
                } else {
                    finalInstruction = finalInstruction + " (" + modsString + ")";
                }
            }

            KOTItem kotItem = new KOTItem();
            kotItem.setRestaurantId(restaurantId);
            kotItem.setMenuItemId(menuItem.getId());
            kotItem.setItemName(menuItem.getName());
            kotItem.setQuantity(itemReq.getQuantity());
            kotItem.setSpecialInstruction(finalInstruction);
            
            kotItems.add(kotItem);

            BigDecimal singleItemPriceWithModifiers = menuItem.getPrice().add(itemSpecificModifiersPrice);
            BigDecimal itemTotal = singleItemPriceWithModifiers.multiply(BigDecimal.valueOf(itemReq.getQuantity()))
                    .add(globalModifiersPrice);
            kotSubtotal = kotSubtotal.add(itemTotal);
        }

        kot.setItems(kotItems);
        KOT savedKot = kotRepository.save(kot);

        // 5. Update overall Order Financials & Taxes (CGST 2.5% + SGST 2.5%)
        updateOrderBilling(order, kotSubtotal);

        // Ensure table status is in RUNNING state (e.g. if new items are added to an existing order)
        if (table.getStatus() != TableStatus.RUNNING) {
            table.setStatus(TableStatus.RUNNING);
            tableRepository.save(table);
        }
 
        // 📢 6. REAL-TIME BROADCAST (WEB-SOCKET PUSH)
        // Defer sending WebSocket notifications until AFTER transaction commits to prevent KDS REST API race condition
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        sendWebSocketNotifications(restaurantId, savedKot, table, order);
                    }
                }
            );
        } else {
            sendWebSocketNotifications(restaurantId, savedKot, table, order);
        }

        return savedKot;
    }

    private void sendWebSocketNotifications(UUID restaurantId, KOT savedKot, DiningTable table, Order order) {
        try {
            // Sends the KOT details instantly to /topic/kitchen/{restaurantId}
            String kitchenTopic = "/topic/kitchen/" + restaurantId;
            messagingTemplate.convertAndSend(kitchenTopic, savedKot);
            System.out.println("📢 WebSocket KOT Broadcast sent to topic: " + kitchenTopic);

            // Sends the table update details instantly to /topic/tables/{restaurantId}
            String tableTopic = "/topic/tables/" + restaurantId;
            Map<String, Object> tablePayload = new HashMap<>();
            tablePayload.put("id", table.getId().toString());
            tablePayload.put("status", table.getStatus().name());
            tablePayload.put("totalAmount", order.getGrandTotal() != null ? order.getGrandTotal().doubleValue() : 0.0);
            if (order.getStartedAt() != null) {
                tablePayload.put("durationMinutes", (int) java.time.Duration.between(order.getStartedAt(), java.time.LocalDateTime.now()).toMinutes());
            } else {
                tablePayload.put("durationMinutes", 0);
            }
            messagingTemplate.convertAndSend(tableTopic, tablePayload);
            System.out.println("📢 WebSocket Table Update Broadcast sent to topic: " + tableTopic);
        } catch (Exception e) {
            System.err.println("❌ Failed to send WebSocket notifications: " + e.getMessage());
        }
    }

    private Order createNewOrder(DiningTable table, String notes) {
        UUID restaurantId = TenantContext.getRestaurantId();

        // Turn table status to RUNNING (Orange dot in Biller UI)
        table.setStatus(TableStatus.RUNNING);
        tableRepository.save(table);

        Order order = new Order();
        order.setRestaurantId(restaurantId);
        order.setOrderNumber("#" + (System.currentTimeMillis() % 10000));
        order.setTableId(table.getId());
        order.setTableName(table.getTableNumber());
        order.setType(OrderType.DINE_IN);
        order.setSource("DIRECT");
        order.setCustomerName("Guest Table " + table.getTableNumber());
        
        if (notes != null && !notes.trim().isEmpty()) {
            order.setNotes(notes);
            order.setCustomerName(notes); // Store notes as session context for compatibility
        }

        return orderRepository.save(order);
    }

    private void updateOrderBilling(Order order, BigDecimal additionalAmount) {
        BigDecimal newSubTotal = order.getSubTotal().add(additionalAmount);
        order.setSubTotal(newSubTotal);

        // 5% total tax (2.5% CGST + 2.5% SGST)
        BigDecimal taxRate = BigDecimal.valueOf(0.025);
        BigDecimal cgst = newSubTotal.multiply(taxRate);
        BigDecimal sgst = newSubTotal.multiply(taxRate);

        order.setCgst(cgst);
        order.setSgst(sgst);
        order.setGrandTotal(newSubTotal.add(cgst).add(sgst));

        orderRepository.save(order);
    }

    @Transactional
    public Order createMergedOrder(List<UUID> tableIds, String notes) {
        UUID restaurantId = TenantContext.getRestaurantId();

        if (tableIds == null || tableIds.isEmpty()) {
            throw new RuntimeException("No tables selected for merging");
        }

        List<DiningTable> tables = tableRepository.findAllById(tableIds);
        if (tables.isEmpty()) {
            throw new RuntimeException("Selected tables not found");
        }

        List<String> tableNames = new ArrayList<>();
        List<String> idStrings = new ArrayList<>();

        for (DiningTable table : tables) {
            if (!table.getRestaurantId().equals(restaurantId)) {
                throw new RuntimeException("Security Violation: Table belongs to another tenant");
            }
            if (table.getStatus() != TableStatus.AVAILABLE) {
                throw new RuntimeException("Table " + table.getTableNumber() + " is not vacant");
            }

            // Turn merged tables to RUNNING status
            table.setStatus(TableStatus.RUNNING);
            tableRepository.save(table);

            tableNames.add(table.getTableNumber());
            idStrings.add(table.getId().toString());
        }

        // Sort table numbers so they display consistently e.g., T-01 + T-02
        tableNames.sort(String::compareTo);

        // Create unified "MERGE" Order
        Order order = new Order();
        order.setRestaurantId(restaurantId);
        order.setOrderNumber("#" + (System.currentTimeMillis() % 10000));
        order.setTableId(tables.get(0).getId()); // Primary anchor table
        
        // Dynamic name: e.g. MERGE 1, MERGE 2...
        List<Order> activeOrders = orderRepository.findByRestaurantIdAndStatusNotIn(restaurantId, java.util.Arrays.asList(OrderStatus.PAID, OrderStatus.CANCELLED));
        java.util.Set<Integer> activeMergeNums = new java.util.HashSet<>();
        for (Order o : activeOrders) {
            if (o.getTableName() != null && o.getTableName().startsWith("MERGE ")) {
                try {
                    int num = Integer.parseInt(o.getTableName().substring(6).trim());
                    activeMergeNums.add(num);
                } catch (Exception ignored) {}
            }
        }
        int nextMergeNum = 1;
        while (activeMergeNums.contains(nextMergeNum)) {
            nextMergeNum++;
        }
        String mergedName = "MERGE " + nextMergeNum;
        order.setTableName(mergedName);
        order.setCustomerName(mergedName);
        order.setMergedTableIds(String.join(",", idStrings));
        order.setType(OrderType.DINE_IN);
        order.setSource("DIRECT");
        if (notes != null && !notes.trim().isEmpty()) {
            order.setNotes(notes);
        }

        Order savedOrder = orderRepository.save(order);

        // Defer WebSocket broadcasts until AFTER transaction commits
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        broadcastMergedTablesUpdate(restaurantId, tables, savedOrder);
                    }
                }
            );
        } else {
            broadcastMergedTablesUpdate(restaurantId, tables, savedOrder);
        }

        return savedOrder;
    }

    @Transactional
    public KOT cancelKOTItem(UUID kotId, UUID kotItemId) {
        UUID restaurantId = TenantContext.getRestaurantId();

        KOT kot = kotRepository.findById(kotId)
                .orElseThrow(() -> new RuntimeException("KOT not found"));

        if (!kot.getRestaurantId().equals(restaurantId)) {
            throw new RuntimeException("Security Violation: KOT belongs to another tenant");
        }

        KOTItem targetItem = kot.getItems().stream()
                .filter(item -> item.getId().equals(kotItemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("KOT Item not found"));

        // Check if the item is already cooked (READY or SERVED)
        if (targetItem.getItemStatus() == KOTStatus.READY || targetItem.getItemStatus() == KOTStatus.SERVED) {
            throw new RuntimeException("Cannot cancel an item that is already prepared or served");
        }

        if (targetItem.getItemStatus() == KOTStatus.CANCELLED) {
            throw new RuntimeException("Item is already cancelled");
        }

        // 1. Cancel the item
        targetItem.setItemStatus(KOTStatus.CANCELLED);

        // 2. Recalculate KOT subtotal and reduce it from the Order overall amount
        MenuItem menuItem = menuRepository.findById(targetItem.getMenuItemId())
                .orElseThrow(() -> new RuntimeException("Menu item not found: " + targetItem.getMenuItemId()));

        BigDecimal deduction = menuItem.getPrice().multiply(BigDecimal.valueOf(targetItem.getQuantity()));

        // 3. Find the Order and deduct the amount
        Order order = orderRepository.findById(kot.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        updateOrderBilling(order, deduction.negate());

        // 4. Update overall status of the KOT. If all items are CANCELLED, set overallStatus to CANCELLED.
        boolean allCancelled = true;
        for (KOTItem item : kot.getItems()) {
            if (item.getItemStatus() != KOTStatus.CANCELLED) {
                allCancelled = false;
                break;
            }
        }
        if (allCancelled) {
            kot.setOverallStatus(KOTStatus.CANCELLED);
        }

        KOT savedKot = kotRepository.save(kot);

        // 5. If all items in all KOTs for this order are cancelled, vacant the table
        // We will keep table running to let waiter settle manually, or if order subtotal is <= 0
        DiningTable table = tableRepository.findById(kot.getTableId()).orElse(null);

        // 📢 6. REAL-TIME BROADCAST (WEB-SOCKET PUSH)
        if (table != null) {
            if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            sendWebSocketNotifications(restaurantId, savedKot, table, order);
                        }
                    }
                );
            } else {
                sendWebSocketNotifications(restaurantId, savedKot, table, order);
            }
        }

        return savedKot;
    }

    @Transactional
    public KOT updateKOTItemStatus(UUID kotId, UUID kotItemId, KOTStatus newStatus) {
        UUID restaurantId = TenantContext.getRestaurantId();

        KOT kot = kotRepository.findById(kotId)
                .orElseThrow(() -> new RuntimeException("KOT not found"));

        if (!kot.getRestaurantId().equals(restaurantId)) {
            throw new RuntimeException("Security Violation: KOT belongs to another tenant");
        }

        KOTItem targetItem = kot.getItems().stream()
                .filter(item -> item.getId().equals(kotItemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("KOT Item not found"));

        targetItem.setItemStatus(newStatus);

        // Transition KOT overallStatus based on items
        boolean anyPreparingOrReady = false;
        boolean allServedOrCancelled = true;
        boolean anyServed = false;

        for (KOTItem item : kot.getItems()) {
            if (item.getItemStatus() == KOTStatus.PREPARING || item.getItemStatus() == KOTStatus.READY) {
                anyPreparingOrReady = true;
            }
            if (item.getItemStatus() != KOTStatus.SERVED && item.getItemStatus() != KOTStatus.CANCELLED) {
                allServedOrCancelled = false;
            }
            if (item.getItemStatus() == KOTStatus.SERVED) {
                anyServed = true;
            }
        }

        if (allServedOrCancelled) {
            kot.setOverallStatus(anyServed ? KOTStatus.SERVED : KOTStatus.CANCELLED);
        } else if (anyPreparingOrReady && kot.getOverallStatus() == KOTStatus.PENDING) {
            kot.setOverallStatus(KOTStatus.PREPARING);
        }

        KOT savedKot = kotRepository.save(kot);

        // Broadcast to WebSocket
        DiningTable table = tableRepository.findById(kot.getTableId()).orElse(null);
        if (table != null) {
            Order order = orderRepository.findById(kot.getOrderId()).orElse(null);
            if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            sendWebSocketNotifications(restaurantId, savedKot, table, order);
                        }
                    }
                );
            } else {
                sendWebSocketNotifications(restaurantId, savedKot, table, order);
            }
        }

        return savedKot;
    }

    @Transactional
    public KOT cancelWholeKOT(UUID kotId) {
        UUID restaurantId = TenantContext.getRestaurantId();

        KOT kot = kotRepository.findById(kotId)
                .orElseThrow(() -> new RuntimeException("KOT not found"));

        if (!kot.getRestaurantId().equals(restaurantId)) {
            throw new RuntimeException("Security Violation: KOT belongs to another tenant");
        }

        // Check if ANY item in the KOT is already cooked (READY or SERVED)
        for (KOTItem item : kot.getItems()) {
            if (item.getItemStatus() == KOTStatus.READY || item.getItemStatus() == KOTStatus.SERVED) {
                throw new RuntimeException("Cannot cancel KOT: some items are already prepared or served");
            }
        }

        BigDecimal totalDeduction = BigDecimal.ZERO;
        for (KOTItem item : kot.getItems()) {
            if (item.getItemStatus() != KOTStatus.CANCELLED) {
                item.setItemStatus(KOTStatus.CANCELLED);

                MenuItem menuItem = menuRepository.findById(item.getMenuItemId()).orElse(null);
                if (menuItem != null) {
                    totalDeduction = totalDeduction.add(menuItem.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            }
        }

        kot.setOverallStatus(KOTStatus.CANCELLED);

        // Deduct from Order
        Order order = orderRepository.findById(kot.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        updateOrderBilling(order, totalDeduction.negate());

        KOT savedKot = kotRepository.save(kot);

        DiningTable table = tableRepository.findById(kot.getTableId()).orElse(null);

        // Broadcast updates
        if (table != null) {
            if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            sendWebSocketNotifications(restaurantId, savedKot, table, order);
                        }
                    }
                );
            } else {
                sendWebSocketNotifications(restaurantId, savedKot, table, order);
            }
        }

        return savedKot;
    }



    private void broadcastMergedTablesUpdate(UUID restaurantId, List<DiningTable> tables, Order order) {
        try {
            String tableTopic = "/topic/tables/" + restaurantId;
            for (DiningTable table : tables) {
                Map<String, Object> tablePayload = new HashMap<>();
                tablePayload.put("id", table.getId().toString());
                tablePayload.put("status", table.getStatus().name());
                tablePayload.put("totalAmount", order.getGrandTotal() != null ? order.getGrandTotal().doubleValue() : 0.0);
                if (order.getStartedAt() != null) {
                    tablePayload.put("durationMinutes", (int) java.time.Duration.between(order.getStartedAt(), java.time.LocalDateTime.now()).toMinutes());
                } else {
                    tablePayload.put("durationMinutes", 0);
                }
                messagingTemplate.convertAndSend(tableTopic, tablePayload);
            }
            System.out.println("📢 WebSocket Broadcast sent for merged tables on topic: " + tableTopic);
        } catch (Exception e) {
            System.err.println("❌ Failed to broadcast merged tables update: " + e.getMessage());
        }
    }
}
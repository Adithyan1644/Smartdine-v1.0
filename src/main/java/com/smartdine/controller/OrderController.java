package com.smartdine.controller;

import com.smartdine.coreheart.KOT;
import com.smartdine.coreheart.Order;
import com.smartdine.dto.OrderRequest;
import com.smartdine.dto.MergeTableRequest;
import com.smartdine.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/waiter/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<KOT> placeOrder(@RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.processNewKOT(request));
    }

    @PostMapping("/merge")
    public ResponseEntity<Order> mergeTables(@RequestBody MergeTableRequest request) {
        return ResponseEntity.ok(orderService.createMergedOrder(request.getTableIds(), request.getNotes()));
    }

    @DeleteMapping("/kots/{kotId}/items/{kotItemId}")
    public ResponseEntity<KOT> cancelKOTItem(@PathVariable java.util.UUID kotId, @PathVariable java.util.UUID kotItemId) {
        return ResponseEntity.ok(orderService.cancelKOTItem(kotId, kotItemId));
    }

    @DeleteMapping("/kots/{kotId}")
    public ResponseEntity<KOT> cancelWholeKOT(@PathVariable java.util.UUID kotId) {
        return ResponseEntity.ok(orderService.cancelWholeKOT(kotId));
    }

    @PutMapping("/kots/{kotId}/items/{kotItemId}/status")
    public ResponseEntity<KOT> updateKOTItemStatus(
            @PathVariable java.util.UUID kotId,
            @PathVariable java.util.UUID kotItemId,
            @RequestBody java.util.Map<String, String> body) {
        String statusStr = body.get("status");
        if (statusStr == null) {
            throw new IllegalArgumentException("Status value is missing");
        }
        com.smartdine.coreheart.KOTStatus status = com.smartdine.coreheart.KOTStatus.valueOf(statusStr.toUpperCase());
        return ResponseEntity.ok(orderService.updateKOTItemStatus(kotId, kotItemId, status));
    }

    @PutMapping("/{orderId}/priority")
    public ResponseEntity<Order> updatePriority(
            @PathVariable java.util.UUID orderId,
            @RequestBody(required = false) java.util.Map<String, Boolean> body) {
        if (body != null && body.containsKey("priority")) {
            return ResponseEntity.ok(orderService.updateOrderPriority(orderId, body.get("priority")));
        } else {
            return ResponseEntity.ok(orderService.toggleOrderPriority(orderId));
        }
    }
}
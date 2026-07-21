package com.smartdine.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdine.coreheart.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.*;

/**
 * Direction A: Local-to-Cloud Sync (Asynchronous Sales Archiving & Auto-Recovery)
 * Automatically archives settled sales from the local Billing PC to Google Cloud SQL.
 * Includes local offline disk caching and auto-recovery bulk upload when internet returns.
 */
@Service
@Profile("!prod") // Active on local restaurant Billing PC
public class CloudSyncService {

    private final String CLOUD_SYNC_URL = "https://smartdine-v1-0-git-635032287458.europe-west1.run.app/api/sync/orders";
    private final String CLOUD_BULK_SYNC_URL = "https://smartdine-v1-0-git-635032287458.europe-west1.run.app/api/sync/orders/bulk";
    private final File offlineQueueFile = new File("offline-sales-queue.json");

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Triggers asynchronous background sync for a settled order.
     * Runs in a Virtual Thread (@Async) so cashier checkout is 100% instant.
     */
    @Async
    public void syncOrderToCloud(Order order) {
        if (order == null) return;

        Map<String, Object> payload = buildOrderPayload(order);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Restaurant-ID", order.getRestaurantId() != null ? order.getRestaurantId().toString() : "");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(CLOUD_SYNC_URL, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("☁️ Cloud Sync: Archived settled bill " + order.getOrderNumber() + " to Google Cloud SQL.");
            } else {
                saveToOfflineQueue(payload);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Local: Internet connection offline (" + e.getMessage() + "). Archived bill " + order.getOrderNumber() + " locally for auto cloud push.");
            saveToOfflineQueue(payload);
        }
    }

    /**
     * Periodically flushes offline queued bills to Google Cloud SQL when internet connectivity returns.
     */
    @Scheduled(fixedDelay = 30000)
    public synchronized void flushOfflineQueue() {
        if (!offlineQueueFile.exists() || offlineQueueFile.length() == 0) {
            return;
        }

        try {
            List<Map<String, Object>> queuedOrders = objectMapper.readValue(offlineQueueFile, new TypeReference<List<Map<String, Object>>>() {});
            if (queuedOrders.isEmpty()) return;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(queuedOrders, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(CLOUD_BULK_SYNC_URL, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("🚀 Local: Internet restored! Bulk uploaded " + queuedOrders.size() + " offline sales to Google Cloud SQL.");
                // Clear queue file
                objectMapper.writeValue(offlineQueueFile, Collections.emptyList());
            }
        } catch (Exception e) {
            // Internet is still offline or server unreachable — keep retry silent until restored
        }
    }

    private synchronized void saveToOfflineQueue(Map<String, Object> payload) {
        try {
            List<Map<String, Object>> queue = new ArrayList<>();
            if (offlineQueueFile.exists() && offlineQueueFile.length() > 0) {
                try {
                    queue = objectMapper.readValue(offlineQueueFile, new TypeReference<List<Map<String, Object>>>() {});
                } catch (Exception ignored) {}
            }
            queue.add(payload);
            objectMapper.writeValue(offlineQueueFile, queue);
        } catch (Exception e) {
            System.err.println("Error queueing offline sales transaction: " + e.getMessage());
        }
    }

    private Map<String, Object> buildOrderPayload(Order order) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("restaurantId", order.getRestaurantId() != null ? order.getRestaurantId().toString() : "");
        payload.put("orderNumber", order.getOrderNumber());
        payload.put("type", order.getType() != null ? order.getType().name() : "DINE_IN");
        payload.put("source", order.getSource() != null ? order.getSource() : "DIRECT");
        payload.put("status", order.getStatus() != null ? order.getStatus().name() : "PAID");
        payload.put("subTotal", order.getSubTotal());
        payload.put("cgst", order.getCgst());
        payload.put("sgst", order.getSgst());
        payload.put("discount", order.getDiscount());
        payload.put("grandTotal", order.getGrandTotal());
        payload.put("paymentMode", order.getPaymentMode() != null ? order.getPaymentMode() : "CASH");
        payload.put("customerName", order.getCustomerName());
        payload.put("customerPhone", order.getCustomerPhone());
        payload.put("startedAt", order.getStartedAt() != null ? order.getStartedAt().toString() : null);
        payload.put("settledAt", order.getSettledAt() != null ? order.getSettledAt().toString() : java.time.LocalDateTime.now().toString());
        return payload;
    }
}

package com.smartdine.service;

import com.smartdine.coreheart.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudSyncService {

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;

    // Direct points to active GCP App Engine cloud endpoints
    private final String DEV_CLOUD_URL = "https://smartdine-saas.ew.r.appspot.com/api/sync/orders";
    private final String PROD_CLOUD_URL = "https://smartdine-saas.ew.r.appspot.com/api/sync/orders";
    private final String REPORT_IP_URL = "https://smartdine-saas.ew.r.appspot.com/api/public/provision/report-ip";

    @org.springframework.beans.factory.annotation.Autowired
    private ActivationService activationService;

    public CloudSyncService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = new RestTemplate();
    }

    // Runs automatically every 60 seconds to pull menu/table updates from Cloud SQL
    @Scheduled(fixedDelay = 60000)
    public void pullCloudConfiguration() {
        try {
            List<Map<String, Object>> configList = jdbcTemplate.queryForList("SELECT activation_code FROM system_config WHERE is_activated = true LIMIT 1");
            if (configList.isEmpty()) return;
            String syncCode = (String) configList.get(0).get("activation_code");
            if (syncCode == null || syncCode.trim().isEmpty()) return;

            activationService.activateSystem(syncCode.trim(), "https://smartdine-saas.ew.r.appspot.com/api/public/provision");
        } catch (Exception ignored) {}
    }

    // Runs automatically every 5 seconds inside a background virtual thread
    @Scheduled(fixedDelay = 5000)
    public void processLocalOutboxQueue() {
        List<Map<String, Object>> pendingEvents;
        try {
            // Ensure outbox table exists in database
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sync_outbox (" +
                    "id UUID PRIMARY KEY, " +
                    "event_type VARCHAR(50) NOT NULL, " +
                    "payload TEXT NOT NULL, " +
                    "synced BOOLEAN DEFAULT false, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "synced_at TIMESTAMP)");

            pendingEvents = jdbcTemplate.queryForList(
                    "SELECT id, event_type, payload, created_at FROM sync_outbox WHERE synced = false ORDER BY created_at ASC LIMIT 10"
            );
        } catch (Exception e) {
            return;
        }

        if (pendingEvents.isEmpty()) {
            return;
        }

        // Determine if this instance is flagged as a development or production merchant
        boolean isTestSystem = false;
        try {
            Boolean testFlag = jdbcTemplate.queryForObject(
                    "SELECT r.is_test FROM system_config s JOIN restaurants r ON s.restaurant_id = r.id LIMIT 1",
                    Boolean.class
            );
            if (testFlag != null) {
                isTestSystem = testFlag;
            }
        } catch (Exception ignored) {
        }

        String activeGatewayUrl = isTestSystem ? DEV_CLOUD_URL : PROD_CLOUD_URL;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Retrieve local auth token to validate tenant context on GCP
        try {
            String authToken = jdbcTemplate.queryForObject("SELECT cloud_auth_token FROM system_config LIMIT 1", String.class);
            if (authToken != null && !authToken.trim().isEmpty()) {
                headers.set("Authorization", "Bearer " + authToken);
            }
        } catch (Exception ignored) {
        }

        for (Map<String, Object> event : pendingEvents) {
            Object rawId = event.get("id");
            UUID eventId;
            if (rawId instanceof UUID) {
                eventId = (UUID) rawId;
            } else if (rawId != null) {
                eventId = UUID.fromString(rawId.toString());
            } else {
                continue;
            }

            String payloadJson = (String) event.get("payload");

            try {
                // Post payload directly to the active Cloud SQL sync endpoint
                HttpEntity<String> request = new HttpEntity<>(payloadJson, headers);
                restTemplate.postForEntity(activeGatewayUrl, request, String.class);

                // Update outbox state to prevent duplicate processing
                jdbcTemplate.update("UPDATE sync_outbox SET synced = true, synced_at = CURRENT_TIMESTAMP WHERE id = ?", eventId);
                System.out.println("Sync Succeeded: Sent outbox transaction [" + eventId + "] successfully to Google Cloud.");

            } catch (Exception e) {
                System.err.println("Sync Failed: Unable to transmit outbox transaction [" + eventId + "]. Connection held for retry: " + e.getMessage());
                break; // Halt queue processing temporarily until connection is recovered
            }
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void reportLocalIpToCloud() {
        try {
            String localIp = InetAddress.getLocalHost().getHostAddress();
            String restId = null;
            try {
                restId = jdbcTemplate.queryForObject("SELECT restaurant_id FROM system_config LIMIT 1", String.class);
            } catch (Exception ignored) {}

            if (restId != null && !restId.trim().isEmpty()) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-Restaurant-ID", restId.trim());

                HttpEntity<String> request = new HttpEntity<>("", headers);
                try {
                    restTemplate.postForEntity(REPORT_IP_URL + "?ip=" + localIp, request, String.class);
                    System.out.println("📶 [Cloud Sync] Successfully registered local IP (" + localIp + ") on Google Cloud.");
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.err.println("⚠️ Cloud Sync: Failed to register local IP on Google Cloud: " + e.getMessage());
        }
    }

    /**
     * Legacy helper method: Enqueues an order into sync_outbox.
     */
    @Async
    public void syncOrderToCloud(Order order) {
        if (order == null) return;
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sync_outbox (" +
                    "id UUID PRIMARY KEY, " +
                    "event_type VARCHAR(50) NOT NULL, " +
                    "payload TEXT NOT NULL, " +
                    "synced BOOLEAN DEFAULT false, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "synced_at TIMESTAMP)");

            UUID eventId = UUID.randomUUID();
            String payload = buildOrderPayloadJson(order);
            jdbcTemplate.update(
                    "INSERT INTO sync_outbox (id, event_type, payload, synced, created_at) VALUES (?, ?, ?, false, CURRENT_TIMESTAMP)",
                    eventId, "ORDER_SETTLED", payload
            );
        } catch (Exception e) {
            System.err.println("Error enqueuing order into sync_outbox: " + e.getMessage());
        }
    }

    private String buildOrderPayloadJson(Order order) {
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
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(payload);
        } catch (Exception e) {
            return "{}";
        }
    }
}

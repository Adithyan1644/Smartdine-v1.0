package com.smartdine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdine.coreheart.*;
import com.smartdine.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Profile("!prod") // Runs ONLY in local restaurant environments (not on Google Cloud Run)
public class TunnelWebSocketClient implements CommandLineRunner {

    // ✅ Update this to your deployed GCP Cloud Run Domain
    private final String CLOUD_WS_URL = "wss://smartdine-saas.ew.r.appspot.com/ws/tunnel";

    @Autowired private MenuRepository menuRepository;
    @Autowired private TableRepository tableRepository;
    @Autowired private CategoryRepository categoryRepository;
    
    @Autowired 
    private com.smartdine.repository.SystemConfigRepository systemConfigRepository;

    private UUID getActiveRestaurantId() {
        return systemConfigRepository.findAll().stream()
                .findFirst()
                .map(com.smartdine.coreheart.SystemConfig::getRestaurantId)
                .orElse(UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"));
    }
    
    @Autowired 
    private SimpMessagingTemplate localWebSocketTemplate; // Injected for local Wi-Fi broadcasts

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
    private WebSocketSession activeSession;

    @Override
    public void run(String... args) {
        connectToCloudTunnel();
    }

    private synchronized void connectToCloudTunnel() {
        if (activeSession != null && activeSession.isOpen()) {
            return;
        }

        final UUID resolvedRestaurantId = getActiveRestaurantId();
        System.out.println("🔌 Local: Connecting to Cloud Tunnel for restaurant " + resolvedRestaurantId + " at: " + CLOUD_WS_URL);
        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("X-Restaurant-ID", resolvedRestaurantId.toString());

        try {
            URI uri = URI.create(CLOUD_WS_URL + "?restaurantId=" + resolvedRestaurantId.toString());
            client.execute(new TextWebSocketHandler() {
                @Override
                public void afterConnectionEstablished(WebSocketSession session) {
                    activeSession = session;
                    System.out.println("🚀 Local: Connected to Cloud Tunnel successfully! Session: " + session.getId());
                }

                @Override
                public void handleTextMessage(WebSocketSession session, TextMessage message) {
                    try {
                        String payloadStr = message.getPayload();
                        if ("pong".equalsIgnoreCase(payloadStr)) return;

                        Map<String, Object> envelope = objectMapper.readValue(payloadStr, Map.class);
                        String event = (String) envelope.get("event");

                        if ("CONFIG_UPDATE".equals(event)) {
                            String type = (String) envelope.get("type");
                            Object data = envelope.get("data");

                            System.out.println("📢 Local: Received Cloud Config Update: " + type);

                            if ("MENU_ITEM".equals(type)) {
                                MenuItem item = objectMapper.convertValue(data, MenuItem.class);
                                menuRepository.save(item); // Save natively to Postgres
                                
                                // Broadcast instantly to all Waiter & KDS apps over local Wi-Fi!
                                localWebSocketTemplate.convertAndSend("/topic/menu/" + resolvedRestaurantId, item);
                            } else if ("TABLE".equals(type)) {
                                DiningTable table = objectMapper.convertValue(data, DiningTable.class);
                                tableRepository.save(table); // Save natively to Postgres
                                
                                // Refresh local JavaFX Floor Map & apps
                                localWebSocketTemplate.convertAndSend("/topic/tables/" + resolvedRestaurantId, Map.of("event", "TABLES_UPDATED"));
                            } else if ("CATEGORY".equals(type)) {
                                Category category = objectMapper.convertValue(data, Category.class);
                                categoryRepository.save(category); // Save natively to Postgres
                                localWebSocketTemplate.convertAndSend("/topic/menu/" + resolvedRestaurantId, Map.of("event", "CATEGORY_UPDATED"));
                            }
                        } else if ("ONLINE_ORDER".equals(event)) {
                            System.out.println("📦 Local: Received Online Order Webhook via Cloud Tunnel!");
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing cloud sync: " + e.getMessage());
                    }
                }

                @Override
                public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                    activeSession = null;
                    System.out.println("🔌 Local: Cloud Tunnel closed. Status: " + status + ". Reconnecting in 10s...");
                    retryConnection();
                }
            }, headers, uri);
        } catch (Exception e) {
            System.out.println("ℹ️ Local: Cloud Tunnel offline (continuing locally): " + e.getMessage());
            retryConnection();
        }
    }

    private void retryConnection() {
        reconnectScheduler.schedule(this::connectToCloudTunnel, 10, TimeUnit.SECONDS);
    }
}

package com.smartdine.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Local POS-side persistent WebSocket client that establishes an outbound tunnel
 * connection to the Cloud Gateway to receive incoming webhook orders (Zomato/Swiggy).
 * Active only in Local mode (!prod profile).
 */
@Service
@Profile("!prod")
public class TunnelWebSocketClient extends TextWebSocketHandler implements CommandLineRunner {

    @Autowired
    private ActivationService activationService;

    private WebSocketSession session;
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
    private boolean isConnecting = false;

    private static final String DEFAULT_RESTAURANT_ID = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11";
    
    // Configurable endpoint (defaults to localhost:8080 representing Cloud Gateway)
    private static final String GATEWAY_WS_URL = "wss://smartdine-v1-0-git-635032287458.europe-west1.run.app/ws/tunnel";

    @Override
    public void run(String... args) throws Exception {
        // Start connection loop asynchronously to not block main thread startup
        scheduleReconnect(5);
    }

    public synchronized void connect() {
        if (isConnecting || (session != null && session.isOpen())) {
            return;
        }

        isConnecting = true;
        System.out.println("🔌 [TunnelWebSocketClient] Connecting to Cloud Webhook Tunnel: " + GATEWAY_WS_URL);

        try {
            UUID restaurantId = activationService.getSystemConfig()
                    .map(c -> c.getRestaurantId())
                    .orElse(UUID.fromString(DEFAULT_RESTAURANT_ID));

            // Append restaurantId parameter so Cloud knows which restaurant this connection belongs to
            String wsUri = GATEWAY_WS_URL + "?restaurantId=" + restaurantId.toString();

            StandardWebSocketClient client = new StandardWebSocketClient();
            CompletableFuture<WebSocketSession> handshake = client.execute(this, wsUri);
            
            session = handshake.get(5, TimeUnit.SECONDS);
            System.out.println("🚀 [TunnelWebSocketClient] Persistent WebSocket Tunnel established successfully!");
        } catch (Exception e) {
            System.err.println("❌ [TunnelWebSocketClient] Connection failed: " + e.getMessage() + ". Reconnecting in 15s...");
            scheduleReconnect(15);
        } finally {
            isConnecting = false;
        }
    }

    private void scheduleReconnect(int delaySeconds) {
        reconnectExecutor.schedule(this::connect, delaySeconds, TimeUnit.SECONDS);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("🔌 [TunnelWebSocketClient] Connection closed. Status: " + status + ". Reconnecting in 15s...");
        scheduleReconnect(15);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("📥 [TunnelWebSocketClient] Received payload: " + payload);

        if ("pong".equalsIgnoreCase(payload)) {
            return;
        }

        // Parse external order webhook message in format "provider:payload"
        int dividerIndex = payload.indexOf(":");
        if (dividerIndex > 0) {
            String provider = payload.substring(0, dividerIndex);
            String jsonContent = payload.substring(dividerIndex + 1);
            processIncomingOrder(provider, jsonContent);
        }
    }

    private void processIncomingOrder(String provider, String json) {
        System.out.println("📦 [TunnelWebSocketClient] Successfully parsed online order from " + provider);
        // This is where order insertion and JavaFX KOT printing would be triggered.
        System.out.println("Order JSON: " + json);
    }
}

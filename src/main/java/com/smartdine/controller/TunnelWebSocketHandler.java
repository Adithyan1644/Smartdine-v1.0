package com.smartdine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TunnelWebSocketHandler extends TextWebSocketHandler {

    // Thread-safe map storing active restaurant connections: restaurantId -> WebSocketSession
    private static final Map<UUID, WebSocketSession> activeTunnels = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String restaurantIdHeader = session.getHandshakeHeaders().getFirst("X-Restaurant-ID");
        UUID restaurantId = null;

        if (restaurantIdHeader != null && !restaurantIdHeader.trim().isEmpty()) {
            try {
                restaurantId = UUID.fromString(restaurantIdHeader.trim());
            } catch (Exception e) {
                System.err.println("⚠️ [TunnelWebSocketHandler] Invalid X-Restaurant-ID header: " + restaurantIdHeader);
            }
        }

        if (restaurantId == null) {
            URI uri = session.getUri();
            if (uri != null && uri.getQuery() != null) {
                for (String param : uri.getQuery().split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2 && "restaurantId".equalsIgnoreCase(pair[0])) {
                        try {
                            restaurantId = UUID.fromString(pair[1]);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        if (restaurantId != null) {
            activeTunnels.put(restaurantId, session);
            System.out.println("☁️ Cloud: Active secure tunnel registered for Restaurant: " + restaurantId + " | Session: " + session.getId());
        } else {
            System.err.println("⚠️ Cloud: Tunnel connection established without valid restaurantId.");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        activeTunnels.values().remove(session);
        System.out.println("🔌 Cloud: Secure tunnel closed for session: " + session.getId() + " | Status: " + status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if ("ping".equalsIgnoreCase(message.getPayload())) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    public static boolean isTunnelActive(UUID restaurantId) {
        if (activeTunnels.isEmpty()) return false;
        if (restaurantId == null) return !activeTunnels.isEmpty();
        WebSocketSession session = activeTunnels.get(restaurantId);
        return session != null && session.isOpen();
    }

    // Method called by Web Admin or Webhook controllers to push real-time updates down the tunnel
    public boolean sendConfigUpdate(UUID restaurantId, String updateType, Object payloadData) {
        WebSocketSession session = activeTunnels.get(restaurantId);
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> envelope = Map.of(
                    "event", "CONFIG_UPDATE",
                    "type", updateType, // "MENU_ITEM", "TABLE", "CATEGORY"
                    "data", payloadData
                );
                String jsonMessage = objectMapper.writeValueAsString(envelope);
                session.sendMessage(new TextMessage(jsonMessage));
                System.out.println("📢 Cloud: Pushed " + updateType + " update to Restaurant: " + restaurantId);
                return true;
            } catch (IOException e) {
                System.err.println("GCP Tunnel Send Error: " + e.getMessage());
            }
        }
        return false;
    }

    public boolean forwardWebhook(UUID restaurantId, String orderJsonPayload) {
        return forwardWebhook(restaurantId, "AGGREGATOR", orderJsonPayload);
    }

    public boolean forwardWebhook(UUID restaurantId, String provider, String orderJsonPayload) {
        WebSocketSession session = activeTunnels.get(restaurantId);
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> envelope = Map.of(
                    "event", "ONLINE_ORDER",
                    "provider", provider != null ? provider : "AGGREGATOR",
                    "data", orderJsonPayload
                );
                String jsonMessage = objectMapper.writeValueAsString(envelope);
                session.sendMessage(new TextMessage(jsonMessage));
                System.out.println("📢 Cloud: Forwarded online order webhook to Restaurant: " + restaurantId);
                return true;
            } catch (IOException e) {
                System.err.println("GCP Tunnel Webhook Forward Error: " + e.getMessage());
            }
        }
        return false;
    }
}

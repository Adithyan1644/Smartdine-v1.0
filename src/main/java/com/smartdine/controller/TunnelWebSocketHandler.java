package com.smartdine.controller;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cloud-side WebSocket Handler to manage persistent tunnel connections
 * from on-premise local POS machines.
 */
@Component
public class TunnelWebSocketHandler extends TextWebSocketHandler {

    // Store active sessions mapped by restaurant ID
    private final Map<UUID, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        // Extract restaurantId from query parameters: ws://.../ws/tunnel?restaurantId=a0eebc99-9c0b...
        String query = uri.getQuery();
        UUID restaurantId = parseRestaurantId(query);

        if (restaurantId == null) {
            session.close(new CloseStatus(4000, "Missing restaurantId query parameter"));
            return;
        }

        // Save active session
        activeSessions.put(restaurantId, session);
        System.out.println("🚀 [TunnelWebSocketServer] Restaurant POS registered: " + restaurantId + " | Session: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        activeSessions.values().remove(session);
        System.out.println("🔌 [TunnelWebSocketServer] Connection closed: " + session.getId() + " | Status: " + status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Handle heartbeat ping-pong or configuration requests from local POS
        if ("ping".equalsIgnoreCase(message.getPayload())) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    /**
     * Forwards an incoming Zomato/Swiggy webhook down the WebSocket tunnel session.
     */
    public boolean forwardWebhook(UUID restaurantId, String provider, String jsonPayload) {
        WebSocketSession session = activeSessions.get(restaurantId);
        if (session == null || !session.isOpen()) {
            System.err.println("❌ [TunnelWebSocketServer] No active connection found for restaurant: " + restaurantId);
            activeSessions.remove(restaurantId);
            return false;
        }

        try {
            // Build tunnel message wrapper: provider:payload
            String tunnelMessage = provider + ":" + jsonPayload;
            session.sendMessage(new TextMessage(tunnelMessage));
            System.out.println("📤 [TunnelWebSocketServer] Webhook forwarded to restaurant: " + restaurantId + " via session: " + session.getId());
            return true;
        } catch (IOException e) {
            System.err.println("❌ [TunnelWebSocketServer] Error forwarding webhook to restaurant " + restaurantId + ": " + e.getMessage());
            return false;
        }
    }

    private UUID parseRestaurantId(String query) {
        if (query == null || query.isEmpty()) return null;
        try {
            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2 && "restaurantId".equalsIgnoreCase(keyValue[0])) {
                    return UUID.fromString(keyValue[1]);
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ [TunnelWebSocketServer] Failed to parse restaurantId query param: " + e.getMessage());
        }
        return null;
    }
}

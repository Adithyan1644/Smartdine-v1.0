package com.smartdine.coreheart;

import com.smartdine.controller.TunnelWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.socket.config.annotation.*;

/**
 * Tunnel WebSocket Configuration active only on the Cloud Gateway (prod profile).
 */
@Configuration
@EnableWebSocket
@Profile("prod")
public class TunnelWebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private TunnelWebSocketHandler tunnelHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Exposes outbound endpoint for local POS clients to establish a tunnel
        registry.addHandler(tunnelHandler, "/ws/tunnel")
                .setAllowedOrigins("*");
    }
}

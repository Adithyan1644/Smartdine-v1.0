package com.smartdine.coreheart;

import com.smartdine.controller.TunnelWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class TunnelWebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private TunnelWebSocketHandler tunnelWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Registers the endpoint used by the local POS PC to establish the tunnel
        registry.addHandler(tunnelWebSocketHandler, "/ws/tunnel")
                .setAllowedOrigins("*");
    }
}

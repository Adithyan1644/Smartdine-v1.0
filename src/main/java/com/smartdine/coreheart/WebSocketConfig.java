package com.smartdine.coreheart;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker // Enables WebSocket message handling
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // This is the endpoint the Flutter apps and JavaFX will connect to
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*"); // In production, restrict this to your IP range
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enables a simple memory-based message broker to carry messages back to clients
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }
}
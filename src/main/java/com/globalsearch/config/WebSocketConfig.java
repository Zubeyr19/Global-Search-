package com.globalsearch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket Configuration for real-time updates
 *
 * This configuration enables STOMP (Simple Text Oriented Messaging Protocol) over WebSocket
 *
 * Endpoints:
 * - /ws: WebSocket connection endpoint
 * - /app: Application destination prefix (messages from client to server)
 * - /topic: Broker destination prefix (messages from server to clients)
 *
 * Usage from frontend:
 * Connect to: ws://localhost:8080/ws
 * Subscribe to: /topic/notifications/{userId}
 * Subscribe to: /topic/search-updates/{tenantId}
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker
        // Messages with destination prefix /topic will be routed to the broker
        config.enableSimpleBroker("/topic", "/queue");

        // Messages with destination prefix /app will be routed to @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");

        // Set user destination prefix for private messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the /ws endpoint, enabling SockJS fallback options
        // so that alternate transports can be used if WebSocket is not available
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // Configure CORS for WebSocket
                .withSockJS();  // Enable SockJS fallback

        // Also register without SockJS for native WebSocket support
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
}

package com.globalsearch.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

/**
 * WebSocket event listener to handle connection lifecycle events
 */
@Component
@Slf4j
public class WebSocketEventListener {

    /**
     * Handle new WebSocket connection
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        log.info("WebSocket client connected - Session ID: {}", sessionId);

        // You can extract user information from headers if authentication is implemented
        // String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
    }

    /**
     * Handle WebSocket disconnection
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        log.info("WebSocket client disconnected - Session ID: {}", sessionId);

        // Cleanup any session-specific data if needed
    }

    /**
     * Handle subscription to a topic
     */
    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();

        log.debug("Client subscribed - Session ID: {}, Destination: {}", sessionId, destination);
    }

    /**
     * Handle unsubscription from a topic
     */
    @EventListener
    public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String subscriptionId = headerAccessor.getSubscriptionId();

        log.debug("Client unsubscribed - Session ID: {}, Subscription ID: {}", sessionId, subscriptionId);
    }
}

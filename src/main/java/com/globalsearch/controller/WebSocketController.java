package com.globalsearch.controller;

import com.globalsearch.dto.WebSocketMessage;
import com.globalsearch.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * WebSocket Controller for real-time messaging
 *
 * Message mappings:
 * - /app/send: Send a message that will be broadcast
 * - /app/ping: Ping endpoint for testing WebSocket connection
 *
 * REST endpoints for testing:
 * - POST /api/websocket/test: Test endpoint to send notifications
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class WebSocketController {

    private final NotificationService notificationService;

    /**
     * Handle messages sent to /app/send
     * Broadcast to /topic/messages
     */
    @MessageMapping("/send")
    @SendTo("/topic/messages")
    public WebSocketMessage sendMessage(@Payload WebSocketMessage message,
                                        SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("Message received from session {}: {}", sessionId, message.getMessage());
        return message;
    }

    /**
     * Handle ping messages for connection testing
     */
    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public String handlePing(String message) {
        log.debug("Ping received: {}", message);
        return "PONG: " + message;
    }
}

/**
 * REST Controller for WebSocket testing and manual notification triggering
 */
@RestController
@RequestMapping("/api/websocket")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "WebSocket", description = "WebSocket testing and notification endpoints")
class WebSocketTestController {

    private final NotificationService notificationService;

    /**
     * Test endpoint to send a notification to a specific user
     */
    @PostMapping("/test/user")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Test user notification", description = "Send a test notification to a specific user")
    public String testUserNotification(@RequestBody TestNotificationRequest request) {
        log.info("Sending test notification to user {}", request.getUserId());

        WebSocketMessage message = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.ALERT)
                .title("Test Notification")
                .message(request.getMessage())
                .tenantId(request.getTenantId())
                .userId(request.getUserId())
                .severity("INFO")
                .build();

        notificationService.notifyUser(request.getUserId(), message);
        return "Notification sent to user " + request.getUserId();
    }

    /**
     * Test endpoint to send a notification to a tenant
     */
    @PostMapping("/test/tenant")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Test tenant notification", description = "Send a test notification to all users in a tenant")
    public String testTenantNotification(@RequestBody TestNotificationRequest request) {
        log.info("Sending test notification to tenant {}", request.getTenantId());

        WebSocketMessage message = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.SYSTEM_UPDATE)
                .title("Tenant Notification")
                .message(request.getMessage())
                .tenantId(request.getTenantId())
                .severity("INFO")
                .build();

        notificationService.notifyTenant(request.getTenantId(), message);
        return "Notification sent to tenant " + request.getTenantId();
    }

    /**
     * Test endpoint to send a system-wide notification
     */
    @PostMapping("/test/system")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Test system notification", description = "Send a test notification to all connected users")
    public String testSystemNotification(@RequestBody TestNotificationRequest request) {
        log.info("Sending system-wide notification");

        WebSocketMessage message = WebSocketMessage.systemUpdate(request.getMessage(), "INFO");
        notificationService.notifySystem(message);
        return "System notification sent";
    }

    /**
     * Request object for testing notifications
     */
    public static class TestNotificationRequest {
        private Long userId;
        private String tenantId;
        private String message;

        // Getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}

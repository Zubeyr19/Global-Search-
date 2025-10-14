package com.globalsearch.service;

import com.globalsearch.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service for sending real-time notifications via WebSocket
 *
 * Notification channels:
 * - /topic/notifications/{userId}: User-specific notifications
 * - /topic/tenant/{tenantId}: Tenant-wide notifications
 * - /topic/system: System-wide notifications
 * - /topic/search/{tenantId}: Search-related updates for a tenant
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send notification to a specific user
     */
    @Async
    public void notifyUser(Long userId, WebSocketMessage message) {
        try {
            log.debug("Sending notification to user {}: {}", userId, message.getMessage());
            messagingTemplate.convertAndSend("/topic/notifications/" + userId, message);
        } catch (Exception e) {
            log.error("Failed to send notification to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Send notification to all users in a tenant
     */
    @Async
    public void notifyTenant(String tenantId, WebSocketMessage message) {
        try {
            log.debug("Sending notification to tenant {}: {}", tenantId, message.getMessage());
            messagingTemplate.convertAndSend("/topic/tenant/" + tenantId, message);
        } catch (Exception e) {
            log.error("Failed to send notification to tenant {}: {}", tenantId, e.getMessage());
        }
    }

    /**
     * Send system-wide notification to all connected users
     */
    @Async
    public void notifySystem(WebSocketMessage message) {
        try {
            log.debug("Sending system notification: {}", message.getMessage());
            messagingTemplate.convertAndSend("/topic/system", message);
        } catch (Exception e) {
            log.error("Failed to send system notification: {}", e.getMessage());
        }
    }

    /**
     * Notify about search result updates for a tenant
     */
    @Async
    public void notifySearchUpdate(String tenantId, WebSocketMessage message) {
        try {
            log.debug("Sending search update to tenant {}: {}", tenantId, message.getMessage());
            messagingTemplate.convertAndSend("/topic/search/" + tenantId, message);
        } catch (Exception e) {
            log.error("Failed to send search update to tenant {}: {}", tenantId, e.getMessage());
        }
    }

    /**
     * Notify about data changes (create/update/delete)
     */
    @Async
    public void notifyDataChange(String tenantId, String entityType, String action, Object payload) {
        WebSocketMessage message = WebSocketMessage.dataChange(tenantId, entityType, action, payload);
        notifyTenant(tenantId, message);
    }

    /**
     * Notify user about their activity result
     */
    @Async
    public void notifyUserActivity(Long userId, String tenantId, String activityMessage) {
        WebSocketMessage message = WebSocketMessage.userActivity(tenantId, userId, activityMessage);
        notifyUser(userId, message);
    }

    /**
     * Send alert to tenant users
     */
    @Async
    public void sendAlert(String tenantId, String alertMessage, String severity) {
        WebSocketMessage message = WebSocketMessage.alert(tenantId, alertMessage, severity);
        notifyTenant(tenantId, message);
    }

    /**
     * Send system-wide alert
     */
    @Async
    public void sendSystemAlert(String alertMessage, String severity) {
        WebSocketMessage message = WebSocketMessage.systemUpdate(alertMessage, severity);
        notifySystem(message);
    }
}

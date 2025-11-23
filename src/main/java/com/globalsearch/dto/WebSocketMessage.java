package com.globalsearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WebSocket message for real-time notifications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    /**
     * Message type enum
     */
    public enum MessageType {
        SEARCH_RESULT,      // New search result available
        USER_ACTIVITY,      // User performed an action
        SYSTEM_UPDATE,      // System-level update
        DATA_CHANGE,        // Data was created/updated/deleted
        ALERT,              // Important alert/notification
        STATUS_CHANGE       // Status change notification
    }

    private MessageType type;
    private String title;
    private String message;
    private Object payload;
    private String tenantId;
    private Long userId;
    private LocalDateTime timestamp;
    private String severity;  // INFO, WARNING, ERROR, SUCCESS

    /**
     * Create a search result notification
     */
    public static WebSocketMessage searchResult(String tenantId, String message, Object payload) {
        return WebSocketMessage.builder()
                .type(MessageType.SEARCH_RESULT)
                .title("New Search Result")
                .message(message)
                .payload(payload)
                .tenantId(tenantId)
                .timestamp(LocalDateTime.now())
                .severity("INFO")
                .build();
    }

    /**
     * Create a user activity notification
     */
    public static WebSocketMessage userActivity(String tenantId, Long userId, String message) {
        return WebSocketMessage.builder()
                .type(MessageType.USER_ACTIVITY)
                .title("User Activity")
                .message(message)
                .tenantId(tenantId)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .severity("INFO")
                .build();
    }

    /**
     * Create a data change notification
     */
    public static WebSocketMessage dataChange(String tenantId, String entityType, String action, Object payload) {
        return WebSocketMessage.builder()
                .type(MessageType.DATA_CHANGE)
                .title(entityType + " " + action)
                .message(String.format("%s has been %s", entityType, action))
                .payload(payload)
                .tenantId(tenantId)
                .timestamp(LocalDateTime.now())
                .severity("INFO")
                .build();
    }

    /**
     * Create a system update notification
     */
    public static WebSocketMessage systemUpdate(String message, String severity) {
        return WebSocketMessage.builder()
                .type(MessageType.SYSTEM_UPDATE)
                .title("System Update")
                .message(message)
                .timestamp(LocalDateTime.now())
                .severity(severity)
                .build();
    }

    /**
     * Create an alert notification
     */
    public static WebSocketMessage alert(String tenantId, String message, String severity) {
        return WebSocketMessage.builder()
                .type(MessageType.ALERT)
                .title("Alert")
                .message(message)
                .tenantId(tenantId)
                .timestamp(LocalDateTime.now())
                .severity(severity)
                .build();
    }
}

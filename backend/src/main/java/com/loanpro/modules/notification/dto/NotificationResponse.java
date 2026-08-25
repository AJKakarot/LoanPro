package com.loanpro.modules.notification.dto;

import com.loanpro.modules.notification.domain.Notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String title,
        String message,
        String type,
        boolean read,
        UUID applicationId,
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),
                notification.getApplication() != null ? notification.getApplication().getId() : null,
                notification.getCreatedAt()
        );
    }
}

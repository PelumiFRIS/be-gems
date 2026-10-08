package com.fris.begems.notification.dto;

import com.fris.begems.notification.Notification;
import com.fris.begems.notification.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationSummary(UUID id, NotificationType type, String title, String body, String link,
        boolean read, Instant createdAt) {

    public static NotificationSummary from(Notification notification) {
        return new NotificationSummary(notification.getId(), notification.getType(), notification.getTitle(),
                notification.getBody(), notification.getLink(), notification.getReadAt() != null,
                notification.getCreatedAt());
    }
}

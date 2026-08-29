package com.clothing.ai.notification.dto;

import java.time.Instant;
import java.util.UUID;

public class NotificationDtos {
    public record NotificationResponse(UUID id, String type, String title, String body, String payload,
                                         boolean read, Instant readAt, String channel, Instant createdAt) {}
}

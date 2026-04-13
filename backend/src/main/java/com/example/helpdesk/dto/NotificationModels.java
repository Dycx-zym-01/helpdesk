package com.example.helpdesk.dto;

import java.time.LocalDateTime;

public final class NotificationModels {

    private NotificationModels() {
    }

    public record NotificationResponse(
            Long id,
            String title,
            String content,
            String typeKey,
            String actorName,
            boolean read,
            Long ticketId,
            String ticketNo,
            LocalDateTime createdAt
    ) {
    }
}

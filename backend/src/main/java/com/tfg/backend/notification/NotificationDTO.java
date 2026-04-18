package com.tfg.backend.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
public class NotificationDTO {
    private String id;
    private String subject;
    private String description;
    private Instant timestamp;
    private boolean isRead;

    // EL NUEVO CAMPO
    private NotificationType type;
}
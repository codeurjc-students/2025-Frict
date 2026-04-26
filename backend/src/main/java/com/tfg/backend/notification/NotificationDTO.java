package com.tfg.backend.notification;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class NotificationDTO {
    private String id;
    private String subject;
    private String description;
    private Instant timestamp;
    private boolean read;
    
    private NotificationType type;

    public NotificationDTO(Notification n) {
        this.id = n.getId();
        this.subject = n.getSubject();
        this.description = n.getDescription();
        this.timestamp = n.getTimestamp();
        this.read = n.isRead();
        this.type = n.getType();
    }
}
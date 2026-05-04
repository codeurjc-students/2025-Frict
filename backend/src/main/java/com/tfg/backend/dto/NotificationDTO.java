package com.tfg.backend.dto;

import com.tfg.backend.model.Notification;
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
    
    private EntityType type;

    public NotificationDTO(Notification n) {
        this.id = n.getId();
        this.subject = n.getSubject();
        this.description = n.getDescription();
        this.timestamp = n.getTimestamp();
        this.read = n.isRead();
        this.type = n.getType();
    }
}
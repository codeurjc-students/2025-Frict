package com.tfg.backend.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "notifications")
@CompoundIndex(name = "username_unread_idx", def = "{'username': 1, 'isRead': 1}")
public class Notification {

    @Id
    private String id;

    @Indexed
    private String username;

    private String subject;
    private String description;

    @Indexed(expireAfter = "30d")
    private Instant timestamp;

    private boolean isRead;

    public Notification(String username, String subject, String description) {
        this.username = username;
        this.subject = subject;
        this.description = description;
        this.timestamp = Instant.now();
        this.isRead = false;
    }
}
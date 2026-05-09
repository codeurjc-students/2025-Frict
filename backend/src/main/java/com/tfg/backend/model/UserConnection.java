package com.tfg.backend.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "connections")
public class UserConnection {

    @Id
    private String username;

    private boolean isOnline;
    private LocalDateTime lastConnected;
    private LocalDateTime lastDisconnected;

    private long lastSessionDurationSeconds;
    private long totalAccumulatedTimeSeconds;

    public UserConnection(String username) {
        this.username = username;
        this.totalAccumulatedTimeSeconds = 0;
    }
}
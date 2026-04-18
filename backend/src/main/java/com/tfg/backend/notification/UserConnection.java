package com.tfg.backend.notification;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
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
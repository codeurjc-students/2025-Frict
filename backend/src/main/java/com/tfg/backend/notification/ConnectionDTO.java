package com.tfg.backend.notification;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionDTO {
    private boolean online;

    @JsonFormat(pattern = "dd/MM/yy HH:mm")
    private LocalDateTime lastConnection;

    private long lastSessionDurationSeconds;
}

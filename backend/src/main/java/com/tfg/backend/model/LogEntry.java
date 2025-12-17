package com.tfg.backend.model;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Embeddable
@Getter
@Setter
public class LogEntry {

    private LocalDateTime date;
    private String description;

    public LogEntry() {
    }

    public LogEntry(String description) {
        this.date = LocalDateTime.now();
        this.description = description;
    }
}

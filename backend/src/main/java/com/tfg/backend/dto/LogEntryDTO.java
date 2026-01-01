package com.tfg.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tfg.backend.model.LogEntry;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LogEntryDTO {

    @JsonFormat(pattern = "dd/MM/yy HH:mm")
    private LocalDateTime date;

    private String description;

    public LogEntryDTO() {
    }

    public LogEntryDTO(LogEntry e) {
        this.date = e.getDate();
        this.description = e.getDescription();
    }
}

package com.tfg.backend.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tfg.backend.model.LogEntry;
import com.tfg.backend.model.StatusLog;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class StatusLogDTO {

    private String status;

    private List<LogEntryDTO> updates = new ArrayList<>();

    public StatusLogDTO() {
    }

    public StatusLogDTO(StatusLog l){
        this.status = l.getStatus().toString();
        for (LogEntry u : l.getUpdates()) {
            this.updates.add(new LogEntryDTO(u));
        }
    }
}

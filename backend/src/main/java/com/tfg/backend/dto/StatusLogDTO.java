package com.tfg.backend.dto;

import com.tfg.backend.model.LogEntry;
import com.tfg.backend.model.StatusLog;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class StatusLogDTO {

    private Long id;

    private String status;

    private List<LogEntryDTO> updates = new ArrayList<>();

    public StatusLogDTO() {
    }

    public StatusLogDTO(StatusLog l){
        this.id = l.getId();
        this.status = l.getStatus().toString();
        for (LogEntry u : l.getUpdates()) {
            this.updates.add(new LogEntryDTO(u));
        }
    }
}

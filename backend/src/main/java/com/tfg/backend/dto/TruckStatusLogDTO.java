package com.tfg.backend.dto;

import com.tfg.backend.model.TruckStatusLog;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TruckStatusLogDTO {

    private Long id;

    private String status;

    private List<LogEntryDTO> updates = new ArrayList<>();

    public TruckStatusLogDTO() {
    }

    public TruckStatusLogDTO(TruckStatusLog l){
        this.id = l.getId();
        this.status = l.getStatus().getDescription();
        this.updates = l.getUpdates().stream().map(LogEntryDTO::new).toList();
    }
}

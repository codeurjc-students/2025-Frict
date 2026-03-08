package com.tfg.backend.dto;

import com.tfg.backend.model.OrderStatusLog;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class OrderStatusLogDTO {

    private Long id;

    private String status;

    private List<LogEntryDTO> updates = new ArrayList<>();

    public OrderStatusLogDTO() {
    }

    public OrderStatusLogDTO(OrderStatusLog l){
        this.id = l.getId();
        this.status = l.getStatus().getDescription();
        this.updates = l.getUpdates().stream().map(LogEntryDTO::new).toList();
    }
}

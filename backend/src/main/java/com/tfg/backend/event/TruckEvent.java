package com.tfg.backend.event;

import com.tfg.backend.dto.EventAction;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TruckEvent {
    private final EventAction action;

    private final String truckId;
    private final String oldStatus;
    private final String newStatus;

    private final String customerUsername;
    private final String managerUsername;
    private final String driverUsername;
}

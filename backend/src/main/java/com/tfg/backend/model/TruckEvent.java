package com.tfg.backend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TruckEvent {
    private final String truckId;
    private final String licensePlate;
    private final String driverUsername;
    private final String targetStoreManagerUsername;
    private final String actorUsername;
    private final String actorRole;
    private final EventAction action;
}

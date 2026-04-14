package com.tfg.backend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderEvent {
    private final EventAction action;

    private final String actorUsername; //For sending exclusions
    private final String actorRole; //For sending policies

    private final String orderId;
    private final String oldStatus;
    private final String newStatus;

    private final String customerUsername;
    private final String managerUsername;
    private final String driverUsername;
}

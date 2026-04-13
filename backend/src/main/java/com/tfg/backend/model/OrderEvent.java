package com.tfg.backend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderEvent {
    private final String orderId;
    private final String oldStatus;
    private final String newStatus;
    private final String managerUsername;    // Destinatario potencial
    private final String driverUsername;     // Destinatario potencial
    private final String actorUsername;      // Quién hizo la acción (para excluirlo)
    private final String actorRole;          // Rol del actor (para aplicar reglas)
    private final EventAction action;
}

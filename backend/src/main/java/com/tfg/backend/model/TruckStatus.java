package com.tfg.backend.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;

public enum TruckStatus {
    AVAILABLE("Disponible", "El camión está listo para comenzar a repartir pedidos."),
    ON_ROUTE("En reparto", "El camión ha iniciado el reparto de pedidos."),
    MAINTENANCE("En mantenimiento", "El camión está siendo revisado o reparado"),
    OUT_OF_SERVICE("Fuera de servicio", "El camión ha sido dado de baja del servicio.");

    private final String description;

    @Getter
    private final String defaultMessage;

    TruckStatus(String description, String defaultMessage) {
        this.description = description;
        this.defaultMessage = defaultMessage;
    }

    @JsonValue
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }

    @JsonCreator
    public static TruckStatus fromDescription(String value) {
        return Arrays.stream(TruckStatus.values())
                .filter(status -> status.description.equalsIgnoreCase(value)
                        || status.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Estado de camión no válido: " + value));
    }
}

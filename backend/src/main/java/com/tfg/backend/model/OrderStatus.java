package com.tfg.backend.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum OrderStatus {
    ORDER_MADE("Pedido Realizado"),
    SENT("Enviado"),
    ON_DELIVERY("En Reparto"),
    COMPLETED("Completado"),
    CANCELLED("Cancelado");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
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
    public static OrderStatus fromDescription(String value) {
        return Arrays.stream(OrderStatus.values())
                .filter(status -> status.description.equalsIgnoreCase(value)
                        || status.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Estado de pedido no válido: " + value));
    }
}
package com.tfg.backend.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;

public enum OrderStatus {
    ORDER_MADE("Pedido Realizado", "El pedido ha sido registrado y está a la espera de ser procesado."),
    SENT("Enviado", "El pedido está siendo procesado."),
    ON_DELIVERY("En Reparto", "El repartidor ha iniciado la ruta de entrega."),
    COMPLETED("Completado", "El pedido ha sido entregado satisfactoriamente."),
    CANCELLED("Cancelado", "El pedido ha sido cancelado.");

    private final String description;

    @Getter
    private final String defaultMessage;

    OrderStatus(String description, String defaultMessage) {
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
    public static OrderStatus fromDescription(String value) {
        return Arrays.stream(OrderStatus.values())
                .filter(status -> status.description.equalsIgnoreCase(value)
                        || status.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Estado de pedido no válido: " + value));
    }
}
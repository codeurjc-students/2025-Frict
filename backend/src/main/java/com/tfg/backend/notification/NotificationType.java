package com.tfg.backend.notification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum NotificationType {
    USER("Usuario"),
    TRUCK("Camión"),
    SHOP("Tienda"),
    ORDER("Pedido"),
    PRODUCT("Producto"),
    REVIEW("Reseña");

    private final String translation;

    NotificationType(String translation) {
        this.translation = translation;
    }

    // Send translation when sending to frontend
    @JsonValue
    public String getTranslation() {
        return translation;
    }

    @Override
    public String toString() {
        return translation;
    }

    // Allows the backend to understand both the original name and the translated name
    @JsonCreator
    public static NotificationType fromTranslation(String value) {
        return Arrays.stream(NotificationType.values())
                .filter(type -> type.translation.equalsIgnoreCase(value)
                        || type.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de notificación no válido: " + value));
    }
}
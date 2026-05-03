package com.tfg.backend.registry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum RegistryType {
    PRODUCT_VIEWS("Visualizaciones"),
    PRODUCT_UNITS_SOLD("Unidades Vendidas"),
    ORDERS_COMPLETED("Pedidos Completados"),
    ORDERS_CANCELLED("Pedidos Cancelados"),
    USER_ORDERS("Pedidos"),
    USER_REVIEWS("Reseñas"),
    SHOP_BUDGET("Presupuesto");

    private final String translation;

    RegistryType(String translation) {
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
    public static RegistryType fromTranslation(String value) {
        return Arrays.stream(RegistryType.values())
                .filter(type -> type.translation.equalsIgnoreCase(value)
                        || type.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de registro no válido: " + value));
    }
}

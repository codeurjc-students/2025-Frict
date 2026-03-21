package com.tfg.backend.utils;

//Although it accepts an object, it is restricted to a String or Number (Autoboxes primitive types)
public record StatDTO(String label, Object value) {

    public StatDTO {
        if (!(value instanceof String || value instanceof Number)) {
            throw new IllegalArgumentException("El valor de la métrica debe ser String o Number");
        }
    }

    public boolean isNumeric() {
        return value instanceof Number;
    }
}

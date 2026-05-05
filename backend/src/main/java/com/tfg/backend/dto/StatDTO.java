package com.tfg.backend.dto;

//Although it accepts an object, it is restricted to a String or Number (Autoboxes primitive types)
public record StatDTO(String label, Object value) {

    public StatDTO {
        if (!(value instanceof String || value instanceof Number)) {
            throw new IllegalArgumentException("The statistic value must be a string or a Number");
        }
    }

    public boolean isNumeric() {
        return value instanceof Number;
    }
}

package com.tfg.backend.dto;

import java.util.Arrays;
import java.util.List;

public record SpecFilterDTO(String name, List<String> values) {

    /** Deserializa el param de URL: "Color:Rojo,Azul" → SpecFilterDTO("Color", ["Rojo","Azul"]) */
    public static SpecFilterDTO fromString(String encoded) {
        String[] parts = encoded.split(":", 2);
        String name = parts[0];
        List<String> values = parts.length > 1
                ? Arrays.asList(parts[1].split(","))
                : List.of();
        return new SpecFilterDTO(name, values);
    }
}

package com.tfg.backend.model;

import com.tfg.backend.notification.EventAction;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ReviewEvent {
    private final EventAction action;
    private final String reviewId; // Puede ser null en CREATED
    private final String productId; // ¡Obligatorio siempre!
    private final List<String> managerUsernames; // La lista que calculas en el servicio
}

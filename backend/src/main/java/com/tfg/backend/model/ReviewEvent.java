package com.tfg.backend.model;

import com.tfg.backend.dto.EventAction;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ReviewEvent {
    private final EventAction action;
    private final String reviewId; // Can be null for CREATED events
    private final String productId;
    private final List<String> managerUsernames;
}

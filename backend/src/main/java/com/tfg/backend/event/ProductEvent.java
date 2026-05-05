package com.tfg.backend.event;

import com.tfg.backend.dto.EventAction;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ProductEvent {
    private final EventAction action;

    private final String productId;

    private final List<String> managerUsernames;
}

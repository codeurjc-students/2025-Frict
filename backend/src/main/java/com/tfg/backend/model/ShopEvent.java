package com.tfg.backend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ShopEvent {
    private final String shopId;
    private final String shopName;
    private final String newManagerUsername;
    private final String actorUsername;
    private final String actorRole;
    private final EventAction action;
}

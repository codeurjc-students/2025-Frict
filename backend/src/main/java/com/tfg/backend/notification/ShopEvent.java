package com.tfg.backend.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ShopEvent {
    private final EventAction action;

    private final String shopId;

    private final boolean notifyCustomers; //All customers that have this shop selected
    private final String managerUsername;
    private final List<String> driverUsernames;
}

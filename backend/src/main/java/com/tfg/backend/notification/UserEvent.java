package com.tfg.backend.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserEvent {
    private final EventAction action;

    private final String targetUsername;
}
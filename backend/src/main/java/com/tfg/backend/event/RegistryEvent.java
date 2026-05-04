package com.tfg.backend.event;


import com.tfg.backend.model.Registry;
import lombok.Getter;

@Getter
public class RegistryEvent {
    private final Registry registry;

    public RegistryEvent(Registry registry) {
        this.registry = registry;
    }
}
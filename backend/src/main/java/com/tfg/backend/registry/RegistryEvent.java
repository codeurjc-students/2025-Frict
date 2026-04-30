package com.tfg.backend.registry;


import lombok.Getter;

@Getter
public class RegistryEvent {
    private final Registry registry;

    public RegistryEvent(Registry registry) {
        this.registry = registry;
    }
}
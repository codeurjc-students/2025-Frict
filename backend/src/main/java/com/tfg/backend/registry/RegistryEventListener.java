package com.tfg.backend.registry;

import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class RegistryEventListener {

    private final MongoTemplate mongoTemplate;

    public RegistryEventListener(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Async
    @EventListener
    public void handleRegistryEvent(RegistryEvent event) {
        mongoTemplate.save(event.getRegistry());
    }
}
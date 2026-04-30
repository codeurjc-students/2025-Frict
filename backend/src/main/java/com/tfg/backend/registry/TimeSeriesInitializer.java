package com.tfg.backend.registry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TimeSeriesInitializer {

    private final MongoTemplate mongoTemplate;

    public TimeSeriesInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (!mongoTemplate.collectionExists(Registry.class)) {
            mongoTemplate.createCollection(Registry.class);
            log.info("Registries time series successfully created.");
        }
    }
}

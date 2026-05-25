package com.tfg.backend.config;

import com.tfg.backend.model.Registry;
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
            try {
                mongoTemplate.createCollection(Registry.class);
                log.info("Registries time series collection created.");
            } catch (Exception e) {
                log.warn("Time series not supported, creating plain collection: {}", e.getMessage());
                mongoTemplate.getDb().createCollection("registries");
                log.info("Registries plain collection created.");
            }
        }
    }
}

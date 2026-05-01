package com.tfg.backend.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
@Slf4j
public class MongoConfig {
    /*
    @Bean
    CommandLineRunner cleanMongoDatabaseOnStartup(MongoTemplate mongoTemplate) {
        return args -> {
            mongoTemplate.getDb().drop();
            log.info("MongoDB 'Frict' deleted successfully (create-drop mode)");
        };
    }
     */
}

package com.tfg.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

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

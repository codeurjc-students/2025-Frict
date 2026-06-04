package com.tfg.backend.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy repairAndMigrate() {
        return (Flyway flyway) -> {
            flyway.repair();
            flyway.migrate();
        };
    }

    // Ensures Hibernate DDL (entityManagerFactory) runs before Flyway migrations,
    // so V2 and later migrations can reference tables created by ddl-auto=update.
    @Bean
    @DependsOn("entityManagerFactory")
    public FlywayMigrationInitializer flywayInitializer(Flyway flyway, FlywayMigrationStrategy strategy) {
        return new FlywayMigrationInitializer(flyway, strategy::migrate);
    }
}

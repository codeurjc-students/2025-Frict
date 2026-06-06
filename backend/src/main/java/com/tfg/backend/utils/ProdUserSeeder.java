package com.tfg.backend.utils;

import com.tfg.backend.model.Category;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.CategoryRepository;
import com.tfg.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class ProdUserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        seedSystemCategories();
    }

    private void seedUsers() {
        seedUserIfAbsent("Usuario",       "user",    "pegog27508@fengnu.com", "pass",        "USER");
        seedUserIfAbsent("Administrador", "admin",   "admin@frict.es",  "adminpass",   "ADMIN");
        seedUserIfAbsent("Gerente",       "manager", "manager@frict.es",     "managerpass", "MANAGER");
        seedUserIfAbsent("Conductor",     "driver",  "driver@frict.es",      "driverpass",  "DRIVER");
    }

    private void seedSystemCategories() {
        seedCategoryIfAbsent("Destacado",  "", "Tendencias",                  "Lo más nuevo",                "Lo que está de moda.");
        seedCategoryIfAbsent("Top Ventas", "", "Los más vendidos",            "Favoritos de la comunidad",   "Éxito garantizado.");
        seedCategoryIfAbsent("Otros",      "", "No clasificados o pendientes", "",                           "");
    }

    private void seedUserIfAbsent(String name, String username, String email, String rawPassword, String role) {
        if (userRepository.findByUsername(username).isPresent()) return;
        try {
            userRepository.save(new User(name, username, email, passwordEncoder.encode(rawPassword), role));
            log.info("Seeded prod user: {}", username);
        } catch (DataIntegrityViolationException e) {
            log.debug("User '{}' already exists (concurrent insert from another replica), skipping.", username);
        }
    }

    private void seedCategoryIfAbsent(String name, String icon, String bannerText, String shortDescription, String longDescription) {
        if (categoryRepository.findByName(name).isPresent()) return;
        try {
            categoryRepository.save(new Category(name, icon, bannerText, shortDescription, longDescription));
            log.info("Seeded system category: {}", name);
        } catch (DataIntegrityViolationException e) {
            log.debug("Category '{}' already exists (concurrent insert from another replica), skipping.", name);
        }
    }
}

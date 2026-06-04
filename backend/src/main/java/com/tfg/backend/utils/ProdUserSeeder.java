package com.tfg.backend.utils;

import com.tfg.backend.model.User;
import com.tfg.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class ProdUserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedIfAbsent("Usuario",       "user",    "pegog27508@fengnu.com", "pass",        "USER");
        seedIfAbsent("Administrador", "admin",   "laxari3928@1200b.com",  "adminpass",   "ADMIN");
        seedIfAbsent("Gerente",       "manager", "manager@gmail.com",     "managerpass", "MANAGER");
        seedIfAbsent("Conductor",     "driver",  "driver@gmail.com",      "driverpass",  "DRIVER");
    }

    private void seedIfAbsent(String name, String username, String email, String rawPassword, String role) {
        if (userRepository.findByUsername(username).isEmpty()) {
            userRepository.save(new User(name, username, email, passwordEncoder.encode(rawPassword), role));
            log.info("Seeded prod user: {}", username);
        }
    }
}

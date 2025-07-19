package com.tfg.backend.repository;

import com.tfg.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String nickname);

    Optional<User> findByEmail(String userEmail);

    Optional<User> findByName(String name);
}

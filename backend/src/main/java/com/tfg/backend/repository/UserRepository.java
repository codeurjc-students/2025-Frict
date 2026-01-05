package com.tfg.backend.repository;

import com.tfg.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    boolean existsByUsernameAndIsBannedTrue(String username);

    boolean existsByUsernameAndIsDeletedTrue(String username);
}

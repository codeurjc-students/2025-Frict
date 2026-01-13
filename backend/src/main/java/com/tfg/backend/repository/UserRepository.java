package com.tfg.backend.repository;

import com.tfg.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsernameAndIsBannedTrue(String username);
    boolean existsByUsernameAndIsDeletedTrue(String username);

    boolean existsByEmailAndIsBannedTrue(String email);
    boolean existsByEmailAndIsDeletedTrue(String email);

    Long countByRolesContaining(String role);
    Long countByIsBannedTrue();
    Long countByIsDeletedTrue();
}

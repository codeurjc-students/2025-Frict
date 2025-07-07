package com.tfg.ProjectBackend.repository;

import com.tfg.ProjectBackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}

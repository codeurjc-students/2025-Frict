package com.tfg.ProjectBackend.repository;

import com.tfg.ProjectBackend.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
}

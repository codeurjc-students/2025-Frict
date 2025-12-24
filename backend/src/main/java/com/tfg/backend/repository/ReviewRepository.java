package com.tfg.backend.repository;

import com.tfg.backend.model.Review;
import com.tfg.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findAllByUser(User user, Pageable pageable);
}

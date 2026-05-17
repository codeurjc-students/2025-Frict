package com.tfg.backend.repository;

import com.tfg.backend.model.Review;
import com.tfg.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findAllByUser(User user, Pageable pageable);

    List<Review> findAllByUser(User user);

    Page<Review> findAllByProductId(Long productId, Pageable pageable);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :pid")
    long countByProductId(@Param("pid") Long productId);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.product.id = :pid")
    double avgRatingByProductId(@Param("pid") Long productId);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.product.id = :pid GROUP BY r.rating")
    List<Object[]> countByProductIdGroupByRating(@Param("pid") Long productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :pid AND r.recommended = true")
    long countRecommendedByProductId(@Param("pid") Long productId);

    @Query("SELECT COUNT(r) > 0 FROM Review r WHERE r.product.id = :pid AND r.user.id = :uid")
    boolean existsByProductIdAndUserId(@Param("pid") Long productId, @Param("uid") Long userId);
}

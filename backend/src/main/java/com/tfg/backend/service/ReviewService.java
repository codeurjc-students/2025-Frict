package com.tfg.backend.service;

import com.tfg.backend.model.Review;
import com.tfg.backend.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    public Optional<Review> findById(Long id){ return reviewRepository.findById(id); }

    public Review save(Review r) {
        return reviewRepository.save(r);
    }

    public void deleteById(Long id) { reviewRepository.deleteById(id); }
}

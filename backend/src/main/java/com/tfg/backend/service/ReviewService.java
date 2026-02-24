package com.tfg.backend.service;

import com.tfg.backend.model.Review;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    public Optional<Review> findById(Long id){ return reviewRepository.findById(id); }

    public List<Review> findAllByUser(User u){
        return reviewRepository.findAllByUser(u);
    }

    public Page<Review> findAllByUser(User u, Pageable pageInfo){
        return reviewRepository.findAllByUser(u, pageInfo);
    }

    public Review save(Review r) {
        return reviewRepository.save(r);
    }

    public void deleteById(Long id) { reviewRepository.deleteById(id); }

    public Review findReviewHelper(Long id) {
        return this.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review with ID " + id + " does not exist."));
    }
}

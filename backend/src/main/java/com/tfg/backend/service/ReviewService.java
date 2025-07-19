package com.tfg.backend.service;

import com.tfg.backend.model.Review;
import com.tfg.backend.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository repository;

    public List<Review> findAll() { return repository.findAll(); }

    public Review save(Review r) {
        return repository.save(r);
    }
}

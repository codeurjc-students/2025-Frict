package com.tfg.ProjectBackend.service;

import com.tfg.ProjectBackend.model.Review;
import com.tfg.ProjectBackend.repository.ReviewRepository;
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

package com.tfg.backend.service;

import com.tfg.backend.model.Product;
import com.tfg.backend.model.Review;
import com.tfg.backend.repository.ProductRepository;
import com.tfg.backend.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    public Review save(Review r) {
        return reviewRepository.save(r);
    }
}

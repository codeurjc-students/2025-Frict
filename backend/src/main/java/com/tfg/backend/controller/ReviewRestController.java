package com.tfg.backend.controller;

import com.tfg.backend.DTO.ReviewDTO;
import com.tfg.backend.DTO.ReviewListDTO;
import com.tfg.backend.model.Product;
import com.tfg.backend.model.Review;
import com.tfg.backend.service.ProductService;
import com.tfg.backend.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewRestController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ProductService productService;

    //Get all the reviews of a product
    @GetMapping("/")
    public ResponseEntity<ReviewListDTO> showAllByProductId(@RequestParam Long productId) {
        Optional<Product> product = productService.findById(productId);
        if (product.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<ReviewDTO> dtos = new ArrayList<>();
        for (Review r : product.get().getReviews()) {
            dtos.add(new ReviewDTO(r));
        }
        return ResponseEntity.ok(new ReviewListDTO(dtos));
    }
}

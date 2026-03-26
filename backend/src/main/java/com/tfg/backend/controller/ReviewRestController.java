package com.tfg.backend.controller;

import com.tfg.backend.dto.PageResponse;
import com.tfg.backend.dto.ReviewDTO;
import com.tfg.backend.model.Review;
import com.tfg.backend.service.ReviewService;
import com.tfg.backend.utils.PageFormatter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/reviews")
@Tag(name = "Review Management", description = "Product reviews management")
public class ReviewRestController {

    @Autowired
    private ReviewService reviewService;

    @Operation(summary = "(Admin) Get user reviews by user ID (paged)")
    @GetMapping("/user/{id}")
    public ResponseEntity<PageResponse<ReviewDTO>> getUserReviewsByUserId (@PathVariable Long id, Pageable pageable){
        Page<Review> userReviews = reviewService.getReviewsByUserId(id, pageable);
        return ResponseEntity.ok(PageFormatter.toPageResponse(userReviews, ReviewDTO::new));
    }


    @Operation(summary = "(All) Get logged user reviews (paged)")
    @GetMapping
    public ResponseEntity<PageResponse<ReviewDTO>> getAllUserReviews(Pageable pageable){
        Page<Review> userReviews = reviewService.getLoggedUserReviews(pageable);
        return ResponseEntity.ok(PageFormatter.toPageResponse(userReviews, ReviewDTO::new));
    }


    //Get all the reviews of a product
    @Operation(summary = "(All) Get all reviews by product ID")
    @GetMapping("/")
    public ResponseEntity<List<ReviewDTO>> getAllReviewsByProductId(@RequestParam Long productId) {
        Set<Review> reviews = reviewService.getReviewsByProductId(productId);
        List<ReviewDTO> dtos = reviews.stream().map(ReviewDTO::new).toList();
        return ResponseEntity.ok(dtos);
    }


    @Operation(summary = "(User) Create review")
    @PostMapping
    public ResponseEntity<ReviewDTO> createReview(@RequestBody ReviewDTO reviewDTO) {
        Review savedReview = reviewService.createReview(reviewDTO);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedReview.getId())
                .toUri();

        return ResponseEntity.created(location).body(new ReviewDTO(savedReview));
    }


    @Operation(summary = "(User) Update review")
    @PutMapping
    public ResponseEntity<ReviewDTO> updateReview(@RequestBody ReviewDTO reviewDTO) {
        Review updatedReview = reviewService.updateReview(reviewDTO);
        return ResponseEntity.ok().body(new ReviewDTO(updatedReview));
    }


    @Operation(summary = "(Admin, User) Delete review by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ReviewDTO> deleteReviewById(@PathVariable Long id) {
        Review deletedReview = reviewService.deleteReview(id);
        return ResponseEntity.ok().body(new ReviewDTO(deletedReview));
    }
}

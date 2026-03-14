package com.tfg.backend.controller;

import com.tfg.backend.dto.PageResponse;
import com.tfg.backend.dto.ReviewDTO;
import com.tfg.backend.model.Product;
import com.tfg.backend.model.Review;
import com.tfg.backend.model.User;
import com.tfg.backend.service.ProductService;
import com.tfg.backend.service.ReviewService;
import com.tfg.backend.service.UserService;
import com.tfg.backend.utils.PageFormatter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@Tag(name = "Review Management", description = "Product reviews management")
public class ReviewRestController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;


    @Operation(summary = "(All) Get logged user reviews (paged)")
    @GetMapping
    public ResponseEntity<PageResponse<ReviewDTO>> getAllUserReviews(Pageable pageable){
        //Get logged user info if any (User class)
        User loggedUser = userService.findLoggedUserHelper();

        Page<Review> userReviews = reviewService.findAllByUser(loggedUser, pageable);
        return ResponseEntity.ok(PageFormatter.toPageResponse(userReviews, ReviewDTO::new));
    }


    //Get all the reviews of a product
    @Operation(summary = "(All) Get all reviews by product ID")
    @GetMapping("/")
    public ResponseEntity<List<ReviewDTO>> showAllByProductId(@RequestParam Long productId) {
        Product product = productService.findProductHelper(productId);
        List<ReviewDTO> dtos = new ArrayList<>();
        for (Review r : product.getReviews()) {
            dtos.add(new ReviewDTO(r));
        }
        return ResponseEntity.ok(dtos);
    }


    @Operation(summary = "(User) Create review")
    @PostMapping
    public ResponseEntity<ReviewDTO> createReview(@RequestBody ReviewDTO reviewDTO) {
        //Check that the logged user and the review creator match
        User loggedUser = userService.findLoggedUserHelper();

        if (!loggedUser.getId().equals(reviewDTO.getCreatorId())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Creator ID " + reviewDTO.getCreatorId() + " and logged user ID " + loggedUser.getId() + " do not match.");
        }

        //Check that the product exists
        Product product = productService.findProductHelper(reviewDTO.getProductId());

        Review savedReview = reviewService.save(new Review(loggedUser, product, reviewDTO.getRating(), reviewDTO.getText(), reviewDTO.isRecommended()));

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
        //Check that the logged user and the review creator match
        User loggedUser = userService.findLoggedUserHelper();

        if (!loggedUser.getId().equals(reviewDTO.getCreatorId())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Creator ID " + reviewDTO.getCreatorId() + " and logged user ID " + loggedUser.getId() + " do not match.");
        }

        //Check that the review exists
        Review review = reviewService.findReviewHelper(reviewDTO.getId());

        review.setText(reviewDTO.getText());
        review.setRating(reviewDTO.getRating());
        review.setRecommended(reviewDTO.isRecommended());
        reviewService.save(review);
        return ResponseEntity.ok().body(new ReviewDTO(review));
    }


    @Operation(summary = "(Admin, User) Delete review by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ReviewDTO> deleteReview(@PathVariable Long id) {
        //Check that the review exists
        Review review = reviewService.findReviewHelper(id);

        //Check that the logged user and the review creator match
        User loggedUser = userService.findLoggedUserHelper();

        if (!loggedUser.getId().equals(review.getUser().getId())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Creator ID " + review.getUser().getId() + " and logged user ID " + loggedUser.getId() + " do not match.");
        }

        reviewService.deleteById(id);
        return ResponseEntity.ok().body(new ReviewDTO(review));
    }
}

package com.tfg.backend.controller;

import com.tfg.backend.DTO.*;
import com.tfg.backend.model.Order;
import com.tfg.backend.model.Product;
import com.tfg.backend.model.Review;
import com.tfg.backend.model.User;
import com.tfg.backend.service.ProductService;
import com.tfg.backend.service.ReviewService;
import com.tfg.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<ReviewsPageDTO> getAllUserReviews(HttpServletRequest request, Pageable pageable){
        //Get logged user info if any (User class)
        User loggedUser = findLoggedUserHelper(request);

        Page<Review> userReviews = reviewService.findAllByUser(loggedUser, pageable);
        return ResponseEntity.ok(toReviewsPageDTO(userReviews));
    }

    //Get all the reviews of a product
    @GetMapping("/")
    public ResponseEntity<ReviewListDTO> showAllByProductId(@RequestParam Long productId) {
        Product product = findProductHelper(productId);
        List<ReviewDTO> dtos = new ArrayList<>();
        for (Review r : product.getReviews()) {
            dtos.add(new ReviewDTO(r));
        }
        return ResponseEntity.ok(new ReviewListDTO(dtos));
    }

    @PostMapping
    public ResponseEntity<ReviewDTO> createReview(HttpServletRequest request, @RequestBody ReviewDTO reviewDTO) {
        //Check that the logged user and the review creator match
        User loggedUser = findLoggedUserHelper(request);

        if (!loggedUser.getId().equals(reviewDTO.getCreatorId())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Creator ID " + reviewDTO.getCreatorId() + " and logged user ID " + loggedUser.getId() + " do not match.");
        }

        //Check that the product exists
        Product product = findProductHelper(reviewDTO.getProductId());

        Review review = new Review(loggedUser, product, reviewDTO.getRating(), reviewDTO.getText(), reviewDTO.isRecommended());
        reviewService.save(review);
        return ResponseEntity.ok().body(new ReviewDTO(review));
    }


    @PutMapping
    public ResponseEntity<ReviewDTO> updateReview(HttpServletRequest request, @RequestBody ReviewDTO reviewDTO) {
        //Check that the logged user and the review creator match
        User loggedUser = findLoggedUserHelper(request);

        if (!loggedUser.getId().equals(reviewDTO.getCreatorId())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Creator ID " + reviewDTO.getCreatorId() + " and logged user ID " + loggedUser.getId() + " do not match.");
        }

        //Check that the review exists
        Review review = findReviewHelper(reviewDTO.getId());

        review.setText(reviewDTO.getText());
        review.setRating(reviewDTO.getRating());
        review.setRecommended(reviewDTO.isRecommended());
        reviewService.save(review);
        return ResponseEntity.ok().body(new ReviewDTO(review));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ReviewDTO> deleteReview(HttpServletRequest request, @PathVariable Long id) {
        //Check that the review exists
        Review review = findReviewHelper(id);

        //Check that the logged user and the review creator match
        User loggedUser = findLoggedUserHelper(request);

        if (!loggedUser.getId().equals(review.getUser().getId())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Creator ID " + review.getUser().getId() + " and logged user ID " + loggedUser.getId() + " do not match.");
        }

        reviewService.deleteById(id);
        return ResponseEntity.ok().body(new ReviewDTO(review));
    }

    private User findLoggedUserHelper(HttpServletRequest request) {
        return this.userService.getLoggedUser(request)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You must be logged to perform this operation."));
    }

    private Product findProductHelper(Long id) {
        return this.productService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product with ID " + id + " does not exist."));
    }

    private Review findReviewHelper(Long id) {
        return this.reviewService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review with ID " + id + " does not exist."));
    }

    //Creates ReviewsPageDTO objects with necessary fields only
    private ReviewsPageDTO toReviewsPageDTO(Page<Review> reviews){
        List<ReviewDTO> dtos = new ArrayList<>();
        for (Review r : reviews.getContent()) {
            ReviewDTO dto = new ReviewDTO(r);
            dtos.add(dto);
        }
        return new ReviewsPageDTO(dtos, reviews.getTotalElements(), reviews.getNumber(), reviews.getTotalPages()-1, reviews.getSize());
    }
}

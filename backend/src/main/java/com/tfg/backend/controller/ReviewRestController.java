package com.tfg.backend.controller;

import com.tfg.backend.DTO.ProductDTO;
import com.tfg.backend.DTO.ReviewDTO;
import com.tfg.backend.DTO.ReviewListDTO;
import com.tfg.backend.model.Product;
import com.tfg.backend.model.Review;
import com.tfg.backend.model.User;
import com.tfg.backend.service.ProductService;
import com.tfg.backend.service.ReviewService;
import com.tfg.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    @PostMapping
    public ResponseEntity<ReviewDTO> createReview(HttpServletRequest request, @RequestBody ReviewDTO reviewDTO) {
        //Check that the logged user and the review creator match
        Optional<User> userOptional = userService.getLoggedUser(request);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        if (!loggedUser.getId().equals(reviewDTO.getCreatorId())){
            return ResponseEntity.status(401).build(); //Unauthorized as not matched
        }

        //Check that the product exists
        Optional<Product> productOptional = productService.findById(reviewDTO.getProductId());
        if(productOptional.isEmpty()){
            return ResponseEntity.notFound().build(); //Product not found
        }

        Review review = new Review(loggedUser, productOptional.get(), reviewDTO.getRating(), reviewDTO.getText(), reviewDTO.isRecommended());
        reviewService.save(review);
        return ResponseEntity.ok().body(new ReviewDTO(review));
    }


    @PutMapping
    public ResponseEntity<ReviewDTO> updateReview(HttpServletRequest request, @RequestBody ReviewDTO reviewDTO) {
        //Check that the logged user and the review creator match
        Optional<User> userOptional = userService.getLoggedUser(request);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        if (!loggedUser.getId().equals(reviewDTO.getCreatorId())){
            return ResponseEntity.status(401).build(); //Unauthorized as not matched
        }

        //Check that the review exists
        Optional<Review> reviewOptional = reviewService.findById(reviewDTO.getId());
        if(reviewOptional.isEmpty()){
            return ResponseEntity.notFound().build(); //Review not found
        }
        Review review = reviewOptional.get();

        review.setText(reviewDTO.getText());
        review.setRating(reviewDTO.getRating());
        review.setRecommended(reviewDTO.isRecommended());
        reviewService.save(review);
        return ResponseEntity.ok().body(new ReviewDTO(review));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ReviewDTO> deleteReview(HttpServletRequest request, @PathVariable Long id) {
        //Check that the review exists
        Optional<Review> reviewOptional = reviewService.findById(id);
        if(reviewOptional.isEmpty()){
            return ResponseEntity.notFound().build(); //Review not found
        }
        Review review = reviewOptional.get();

        //Check that the logged user and the review creator match
        Optional<User> userOptional = userService.getLoggedUser(request);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        if (!loggedUser.getId().equals(review.getUser().getId())){
            return ResponseEntity.status(401).build(); //Unauthorized as not matched
        }

        reviewService.deleteById(id);
        return ResponseEntity.ok().body(new ReviewDTO(review));
    }
}

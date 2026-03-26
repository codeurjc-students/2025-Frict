package com.tfg.backend.service;

import com.tfg.backend.dto.ReviewDTO;
import com.tfg.backend.model.Product;
import com.tfg.backend.model.Review;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    // --- READ-ONLY METHODS ---

    public Optional<Review> findById(Long id) {
        return reviewRepository.findById(id);
    }

    public List<Review> findAllByUser(User u) {
        return reviewRepository.findAllByUser(u);
    }

    public Page<Review> getLoggedUserReviews(Pageable pageInfo) {
        User user = userService.findLoggedUserHelper();
        return reviewRepository.findAllByUser(user, pageInfo);
    }

    public Page<Review> getReviewsByUserId(Long id, Pageable pageInfo) {
        User user = userService.findUserHelper(id);
        return reviewRepository.findAllByUser(user, pageInfo);
    }

    public Set<Review> getReviewsByProductId(Long id) {
        Product product = productService.findProductHelper(id);
        return product.getReviews();
    }

    public Review findReviewHelper(Long id) {
        return this.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review with ID " + id + " does not exist."));
    }

    // --- WRITING METHODS (override Transactional) ---

    @Transactional
    public Review createReview(ReviewDTO dto) {
        User loggedUser = userService.findLoggedUserHelper();

        // Check DTO integrity
        if (!loggedUser.getId().equals(dto.getCreatorId())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Creator ID " + dto.getCreatorId() + " and logged user ID " + loggedUser.getId() + " do not match.");
        }

        Product product = productService.findProductHelper(dto.getProductId());

        Review newReview = new Review(loggedUser, product, dto.getRating(), dto.getText(), dto.isRecommended());
        return reviewRepository.save(newReview);
    }

    @Transactional
    public Review updateReview(ReviewDTO dto) {
        Review review = this.findReviewHelper(dto.getId());
        User loggedUser = userService.findLoggedUserHelper();

        // Check the logged user definitely is the review creator
        if (!loggedUser.getId().equals(review.getUser().getId())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You can only update your own reviews.");
        }

        review.setText(dto.getText());
        review.setRating(dto.getRating());
        review.setRecommended(dto.isRecommended());

        return review; //Updated automatically
    }

    @Transactional
    public Review deleteReview(Long id) {
        Review review = this.findReviewHelper(id);
        User loggedUser = userService.findLoggedUserHelper();

        if (!loggedUser.getRoles().contains("ADMIN") && !loggedUser.getId().equals(review.getUser().getId())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not an administrator or the creator of this review.");
        }

        reviewRepository.delete(review);
        return review;
    }

    @Transactional
    public Review save(Review r) {
        return reviewRepository.save(r);
    }

    @Transactional
    public void deleteById(Long id) {
        reviewRepository.deleteById(id);
    }
}
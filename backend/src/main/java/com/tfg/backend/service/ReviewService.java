package com.tfg.backend.service;

import com.tfg.backend.dto.EntityType;
import com.tfg.backend.dto.EventAction;
import com.tfg.backend.dto.ReviewDTO;
import com.tfg.backend.dto.ReviewStatsDTO;
import com.tfg.backend.event.RegistryEvent;
import com.tfg.backend.model.*;
import com.tfg.backend.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserService userService;
    private final ProductService productService;
    private final ApplicationEventPublisher eventPublisher;


    // --- READ-ONLY METHODS ---
    public Optional<Review> findById(Long id) {
        return reviewRepository.findById(id);
    }

    public Page<Review> getLoggedUserReviews(Pageable pageInfo) {
        User user = userService.findLoggedUserHelper();
        return reviewRepository.findAllByUser(user, pageInfo);
    }

    public Page<Review> getReviewsByUserId(Long id, Pageable pageInfo) {
        User user = userService.findUserHelper(id);
        return reviewRepository.findAllByUser(user, pageInfo);
    }

    public Page<Review> getReviewsByProductId(Long productId, Pageable pageable) {
        productService.findProductHelper(productId);
        return reviewRepository.findAllByProductId(productId, pageable);
    }

    public ReviewStatsDTO getStatsByProductId(Long productId) {
        productService.findProductHelper(productId);
        long total       = reviewRepository.countByProductId(productId);
        double avg       = reviewRepository.avgRatingByProductId(productId);
        long recommended = reviewRepository.countRecommendedByProductId(productId);
        double recPct    = total > 0 ? (recommended * 100.0 / total) : 0.0;

        Map<Integer, Long> starMap = new HashMap<>();
        for (Object[] row : reviewRepository.countByProductIdGroupByRating(productId))
            starMap.put((Integer) row[0], (Long) row[1]);

        boolean userReviewed = false;
        try {
            User u = userService.findLoggedUserHelper();
            userReviewed = reviewRepository.existsByProductIdAndUserId(productId, u.getId());
        } catch (ResponseStatusException ignored) {}

        return new ReviewStatsDTO(total, avg,
            starMap.getOrDefault(5, 0L), starMap.getOrDefault(4, 0L),
            starMap.getOrDefault(3, 0L), starMap.getOrDefault(2, 0L),
            starMap.getOrDefault(1, 0L), recPct, userReviewed);
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

        //Send notifications
        List<String> managerUsernames = product.getShopsStock().stream().map(stock -> stock.getShop().getAssignedManager()).filter(Objects::nonNull).map(User::getUsername).distinct().toList();
        ReviewEvent reviewEvent = new ReviewEvent(EventAction.CREATED, null, String.valueOf(product.getId()), managerUsernames);
        eventPublisher.publishEvent(reviewEvent);

        //Add registries
        Registry reviewRegistry = new Registry(EntityType.USER, RegistryType.USER_REVIEWS, 1.0, loggedUser.getSelectedShop().getReferenceCode(), loggedUser.getSelectedShop().getName(), loggedUser.getUsername(), loggedUser.getName(), product.getReferenceCode(), product.getName(), null, null);
        eventPublisher.publishEvent(new RegistryEvent(reviewRegistry));

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

        //Send notifications
        Product reviewedProduct = review.getProduct();
        List<String> managerUsernames = reviewedProduct.getShopsStock().stream().map(stock -> stock.getShop().getAssignedManager()).filter(Objects::nonNull).map(User::getUsername).distinct().toList();
        ReviewEvent reviewEvent = new ReviewEvent(EventAction.UPDATED, null, String.valueOf(reviewedProduct.getId()), managerUsernames);
        eventPublisher.publishEvent(reviewEvent);

        return review; //Updated automatically
    }

    @Transactional
    public Review deleteReview(Long id) {
        Review review = this.findReviewHelper(id);
        User loggedUser = userService.findLoggedUserHelper();

        if (!loggedUser.getRoles().contains("ADMIN") && !loggedUser.getId().equals(review.getUser().getId())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not an administrator or the creator of this review.");
        }

        //Send notifications
        Product reviewedProduct = review.getProduct();
        List<String> managerUsernames = reviewedProduct.getShopsStock().stream().map(stock -> stock.getShop().getAssignedManager()).filter(Objects::nonNull).map(User::getUsername).distinct().toList();
        ReviewEvent reviewEvent = new ReviewEvent(EventAction.DELETED, null, String.valueOf(reviewedProduct.getId()), managerUsernames);
        eventPublisher.publishEvent(reviewEvent);

        //Add registries
        Registry reviewRegistry = new Registry(EntityType.USER, RegistryType.USER_REVIEWS, -1.0, loggedUser.getSelectedShop().getReferenceCode(), loggedUser.getSelectedShop().getName(), loggedUser.getUsername(), loggedUser.getName(), reviewedProduct.getReferenceCode(), reviewedProduct.getName(), null, null);
        eventPublisher.publishEvent(new RegistryEvent(reviewRegistry));

        reviewRepository.delete(review);
        return review;
    }

    @Transactional
    public Review save(Review r) {
        return reviewRepository.save(r);
    }
}
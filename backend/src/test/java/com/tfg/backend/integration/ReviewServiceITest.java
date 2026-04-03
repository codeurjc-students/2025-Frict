package com.tfg.backend.integration;

import com.tfg.backend.dto.ReviewDTO;
import com.tfg.backend.model.Product;
import com.tfg.backend.model.Review;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.ProductRepository;
import com.tfg.backend.repository.ReviewRepository;
import com.tfg.backend.repository.UserRepository;
import com.tfg.backend.service.ReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests class for the ReviewService.
 * Validates the creation, modification, and deletion of reviews in the MySQL database,
 * strongly focusing on ownership validation and role-based access control (RBAC).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ReviewServiceITest {

    @Autowired private ReviewService reviewService;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;

    private User reviewOwner;
    private User otherUser;
    private User adminUser;
    private Product mockProduct;
    private Review existingReview;

    @BeforeEach
    void setUpComplexScenario() {
        // 1. Create multiple users to test ownership and permissions
        reviewOwner = new User("Owner User", "owner", "owner@test.com", "pass", "USER");
        otherUser = new User("Other User", "other", "other@test.com", "pass", "USER");
        adminUser = new User("Admin User", "admin", "admin@test.com", "pass", "ADMIN");
        userRepository.saveAll(List.of(reviewOwner, otherUser, adminUser));

        // 2. Create a product to be reviewed
        mockProduct = new Product("Test Laptop", "A powerful machine", 1200.0, 900.0);
        mockProduct.setReferenceCode("PROD-REVIEW-123");
        productRepository.save(mockProduct);

        // 3. Create an existing review linked to the owner
        existingReview = new Review(reviewOwner, mockProduct, 4, "Pretty good, but a bit heavy.", true);
        reviewRepository.save(existingReview);
    }

    @AfterEach
    void tearDown() {
        // Clean up the security context to avoid leaking authentication between tests
        SecurityContextHolder.clearContext();
    }

    /**
     * Helper method to authenticate a user in the SecurityContext.
     */
    private void authenticateUser(User user) {
        String role = user.getRoles().contains("ADMIN") ? "ADMIN" : "USER";
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getUsername(), "pass", List.of(new SimpleGrantedAuthority(role)))
        );
    }

    @Test
    @DisplayName("Create Review: Saves correctly when the logged user matches the DTO creator ID")
    void testCreateReview_ValidOwner_SavedInDatabase() {
        authenticateUser(otherUser);

        ReviewDTO dto = new ReviewDTO();
        dto.setCreatorId(otherUser.getId());
        dto.setProductId(mockProduct.getId());
        dto.setRating(5);
        dto.setText("Amazing product!");
        dto.setRecommended(true);

        Review savedReview = reviewService.createReview(dto);

        Review dbReview = reviewRepository.findById(savedReview.getId()).orElseThrow();

        assertAll(
                () -> assertNotNull(dbReview.getId()),
                () -> assertEquals(5, dbReview.getRating()),
                () -> assertEquals("Amazing product!", dbReview.getText()),
                () -> assertEquals(otherUser.getId(), dbReview.getUser().getId(), "The review must be linked to the other user"),
                () -> assertEquals(mockProduct.getId(), dbReview.getProduct().getId(), "The review must be linked to the product")
        );
    }

    @Test
    @DisplayName("Create Review: Throws UNAUTHORIZED if the user tries to create a review on behalf of someone else")
    void testCreateReview_CreatorIdMismatch_ThrowsUnauthorized() {
        authenticateUser(otherUser); // Logged in as Other User

        ReviewDTO dto = new ReviewDTO();
        dto.setCreatorId(reviewOwner.getId()); // Maliciously passing Owner's ID
        dto.setProductId(mockProduct.getId());
        dto.setRating(1);
        dto.setText("Terrible!");
        dto.setRecommended(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reviewService.createReview(dto));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertNotNull(ex.getReason());
        assertTrue(ex.getReason().contains("do not match"));
    }

    @Test
    @DisplayName("Update Review: Owner can successfully modify their own review")
    void testUpdateReview_ByOwner_UpdatesDatabase() {
        authenticateUser(reviewOwner);

        ReviewDTO dto = new ReviewDTO();
        dto.setId(existingReview.getId());
        dto.setRating(5); // Increased rating
        dto.setText("I changed my mind, it's perfect.");
        dto.setRecommended(true);

        reviewService.updateReview(dto);

        Review dbReview = reviewRepository.findById(existingReview.getId()).orElseThrow();

        assertAll(
                () -> assertEquals(5, dbReview.getRating(), "Rating should be updated"),
                () -> assertEquals("I changed my mind, it's perfect.", dbReview.getText(), "Text should be updated")
        );
    }

    @Test
    @DisplayName("Update Review: Throws UNAUTHORIZED when a non-owner attempts to modify it")
    void testUpdateReview_NotOwner_ThrowsUnauthorized() {
        authenticateUser(otherUser); // Another normal user

        ReviewDTO dto = new ReviewDTO();
        dto.setId(existingReview.getId());
        dto.setRating(1);
        dto.setText("Hacked review!");
        dto.setRecommended(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reviewService.updateReview(dto));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertNotNull(ex.getReason());
        assertTrue(ex.getReason().contains("You can only update your own reviews"));
    }

    @Test
    @DisplayName("Delete Review: Owner can successfully delete their own review")
    void testDeleteReview_ByOwner_RemovedFromDatabase() {
        authenticateUser(reviewOwner);

        reviewService.deleteReview(existingReview.getId());

        assertFalse(reviewRepository.existsById(existingReview.getId()), "Review should be deleted from DB");
    }

    @Test
    @DisplayName("Delete Review: ADMIN can successfully delete any user's review")
    void testDeleteReview_ByAdmin_RemovedFromDatabase() {
        authenticateUser(adminUser); // Logged in as ADMIN

        reviewService.deleteReview(existingReview.getId());

        assertFalse(reviewRepository.existsById(existingReview.getId()), "Admin should be able to delete the review");
    }

    @Test
    @DisplayName("Delete Review: Throws UNAUTHORIZED when a non-owner standard user tries to delete it")
    void testDeleteReview_NotOwnerNorAdmin_ThrowsUnauthorized() {
        authenticateUser(otherUser);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reviewService.deleteReview(existingReview.getId()));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertNotNull(ex.getReason());
        assertTrue(ex.getReason().contains("administrator or the creator"));
    }
}
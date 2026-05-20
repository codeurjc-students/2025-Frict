package com.tfg.backend.unit;

import com.tfg.backend.dto.ReviewDTO;
import com.tfg.backend.model.Product;
import com.tfg.backend.model.Review;
import com.tfg.backend.model.Shop;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.ReviewRepository;
import com.tfg.backend.service.ProductService;
import com.tfg.backend.service.ReviewService;
import com.tfg.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceUTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private UserService userService;
    @Mock private ProductService productService;
    @Mock private ApplicationEventPublisher eventPublisher; //Necessary to avoid errors trying to send notifications, but not used

    @InjectMocks
    private ReviewService reviewService;

    private User loggedUser;
    private Product product;
    private Review review;
    private ReviewDTO reviewDTO;

    @BeforeEach
    void setUp() {
        Shop dummyShop = new Shop();
        dummyShop.setReferenceCode("SH-TEST");

        // Setup User
        loggedUser = new User();
        loggedUser.setId(1L);
        loggedUser.setUsername("testUser");
        loggedUser.setRoles(new HashSet<>(List.of("USER")));
        loggedUser.setSelectedShop(dummyShop);
        lenient().when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

        // Setup Product
        product = new Product();
        product.setId(10L);
        product.setReviews(new HashSet<>());

        // Setup Review
        review = new Review();
        review.setId(100L);
        review.setUser(loggedUser);
        review.setProduct(product);
        review.setText("Great product");
        review.setRating(5);
        review.setRecommended(true);

        // Setup DTO
        reviewDTO = new ReviewDTO();
        reviewDTO.setId(100L);
        reviewDTO.setCreatorId(1L);
        reviewDTO.setProductId(10L);
        reviewDTO.setText("Updated text");
        reviewDTO.setRating(4);
        reviewDTO.setRecommended(false);
    }

    // --- HELPER AND DATA RETRIEVAL TESTS ---
    @Nested
    @DisplayName("Tests for Helper and List Retrieval Methods")
    class ReadMethodsTests {

        @Test
        @DisplayName("findReviewHelper returns the review if it exists in DB")
        void findReviewHelper_Success() {
            when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));

            Review result = reviewService.findReviewHelper(100L);

            assertNotNull(result);
            assertEquals(100L, result.getId());
        }

        @Test
        @DisplayName("findReviewHelper throws 404 NOT_FOUND when review is missing")
        void findReviewHelper_ThrowsNotFound() {
            when(reviewRepository.findById(100L)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> reviewService.findReviewHelper(100L));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertEquals("Review with ID 100 does not exist.", ex.getReason());
        }

        @Test
        @DisplayName("getLoggedUserReviews fetches paginated reviews for the current authenticated user")
        void getLoggedUserReviews_Success() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> page = new PageImpl<>(List.of(review));

            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(reviewRepository.findAllByUser(loggedUser, pageable)).thenReturn(page);

            Page<Review> result = reviewService.getLoggedUserReviews(pageable);

            assertEquals(1, result.getTotalElements());
            verify(reviewRepository).findAllByUser(loggedUser, pageable);
        }

        @Test
        @DisplayName("getReviewsByUserId fetches paginated reviews for any specific user ID")
        void getReviewsByUserId_Success() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> page = new PageImpl<>(List.of(review));

            User specificUser = new User();
            specificUser.setId(5L);

            when(userService.findUserHelper(5L)).thenReturn(specificUser);
            when(reviewRepository.findAllByUser(specificUser, pageable)).thenReturn(page);

            Page<Review> result = reviewService.getReviewsByUserId(5L, pageable);

            assertEquals(1, result.getTotalElements());
            verify(reviewRepository).findAllByUser(specificUser, pageable);
        }

        @Test
        @DisplayName("getReviewsByProductId returns the internal Set of reviews directly from the Product entity")
        void getReviewsByProductId_Success() {
            product.getReviews().add(review);
            when(productService.findProductHelper(10L)).thenReturn(product);

        Set<Review> result = reviewService.getReviewsByProductId(10L);

            assertEquals(1, result.size());
            assertTrue(result.contains(review));
        }
    }

    // --- CREATE REVIEW TESTS ---
    @Nested
    @DisplayName("Tests for createReview")
    class CreateReviewTests {

        @Test
        @DisplayName("Throws UNAUTHORIZED if creatorId in DTO is forged and does not match logged user ID")
        void createReview_ThrowsUnauthorized_WhenIdMismatch() {
            reviewDTO.setCreatorId(99L); // Forging ID: Different from loggedUser.getId() (1L)

            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> reviewService.createReview(reviewDTO));

            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
            assertEquals("Creator ID 99 and logged user ID 1 do not match.", ex.getReason());
        }

        @Test
        @DisplayName("Successfully creates and saves a new review binding the logged user and requested product")
        void createReview_Success() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(productService.findProductHelper(10L)).thenReturn(product);
            when(reviewRepository.save(any(Review.class))).thenAnswer(i -> i.getArgument(0));

            Review result = reviewService.createReview(reviewDTO);

            assertNotNull(result);
            assertEquals(loggedUser, result.getUser(), "Review must be assigned to the logged user");
            assertEquals(product, result.getProduct(), "Review must be assigned to the fetched product");
            assertEquals("Updated text", result.getText());
            assertEquals(4, result.getRating());
            assertFalse(result.isRecommended());

            verify(reviewRepository).save(any(Review.class));
        }
    }

    // --- UPDATE REVIEW TESTS ---
    @Nested
    @DisplayName("Tests for updateReview")
    class UpdateReviewTests {

        @Test
        @DisplayName("Throws UNAUTHORIZED if the logged user tries to edit a review they did not create")
        void updateReview_ThrowsUnauthorized_WhenNotCreator() {
            User anotherUser = new User();
            anotherUser.setId(99L);
            review.setUser(anotherUser); // The original review belongs to someone else

            when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> reviewService.updateReview(reviewDTO));

            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
            assertEquals("You can only update your own reviews.", ex.getReason());
        }

        @Test
        @DisplayName("Successfully maps DTO fields to the existing review when user is the verified creator")
        void updateReview_Success() {
            when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

            Review result = reviewService.updateReview(reviewDTO);

            // Assert field mapping
            assertEquals("Updated text", result.getText());
            assertEquals(4, result.getRating());
            assertFalse(result.isRecommended());
        }
    }

    // --- DELETE REVIEW TESTS ---
    @Nested
    @DisplayName("Tests for deleteReview")
    class DeleteReviewTests {

        @Test
        @DisplayName("Throws UNAUTHORIZED if user trying to delete is neither Admin nor the original Creator")
        void deleteReview_ThrowsUnauthorized_WhenNotAdminNorCreator() {
            User commonUser = new User();
            commonUser.setId(5L);
            commonUser.setRoles(new HashSet<>(List.of("USER"))); // Just a standard user

            User creator = new User();
            creator.setId(10L);
            review.setUser(creator); // It belongs to user 10

            when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
            when(userService.findLoggedUserHelper()).thenReturn(commonUser); // User 5 is logged in

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> reviewService.deleteReview(100L));

            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
            assertEquals("You are not an administrator or the creator of this review.", ex.getReason());
        }

        @Test
        @DisplayName("Successfully deletes the review if the logged user is the original Creator")
        void deleteReview_Success_AsCreator() {
            // Setup matches: review.getUser().getId() == loggedUser.getId()
            when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

            reviewService.deleteReview(100L);

            verify(reviewRepository).delete(review);
        }

        @Test
        @DisplayName("Successfully deletes any review if the logged user has the ADMIN role")
        void deleteReview_Success_AsAdmin() {
            User admin = new User();
            admin.setId(88L);
            admin.setRoles(new HashSet<>(List.of("ADMIN")));

            Shop dummyShop = new Shop();
            dummyShop.setReferenceCode("SH-TEST");
            admin.setSelectedShop(dummyShop);

            // The review belongs to user 1, but user 88 (Admin) is calling the action
            when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
            when(userService.findLoggedUserHelper()).thenReturn(admin);

            reviewService.deleteReview(100L);

            // Even without being the creator, the admin role bypasses the restriction
            verify(reviewRepository).delete(review);
        }
    }
}
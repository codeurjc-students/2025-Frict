package com.tfg.backend.controller;

import com.tfg.backend.dto.PageResponse;
import com.tfg.backend.dto.ReviewDTO;
import com.tfg.backend.dto.UserDTO;
import com.tfg.backend.model.Review;
import com.tfg.backend.service.ReviewService;
import com.tfg.backend.service.UserConnectionService;
import com.tfg.backend.utils.PageFormatter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/api/v1/reviews")
@Tag(name = "Review Management", description = "Product reviews management")
@RequiredArgsConstructor
public class ReviewRestController {

    private final ReviewService reviewService;

    // Inject the user connection service for presence enrichment
    private final UserConnectionService userConnectionService;

    @Operation(summary = "(Admin) Get user reviews by user ID (paged)")
    @GetMapping("/user/{id}")
    public ResponseEntity<PageResponse<ReviewDTO>> getUserReviewsByUserId (@PathVariable Long id, Pageable pageable){
        Page<Review> userReviews = reviewService.getReviewsByUserId(id, pageable);
        return ResponseEntity.ok(toEnrichedPageResponse(userReviews));
    }

    @Operation(summary = "(All) Get logged user reviews (paged)")
    @GetMapping
    public ResponseEntity<PageResponse<ReviewDTO>> getAllUserReviews(Pageable pageable){
        Page<Review> userReviews = reviewService.getLoggedUserReviews(pageable);
        return ResponseEntity.ok(toEnrichedPageResponse(userReviews));
    }

    // Get all the reviews of a product
    @Operation(summary = "(All) Get all reviews by product ID")
    @GetMapping("/")
    public ResponseEntity<List<ReviewDTO>> getAllReviewsByProductId(@RequestParam Long productId) {
        Set<Review> reviews = reviewService.getReviewsByProductId(productId);
        return ResponseEntity.ok(toEnrichedDTOList(reviews));
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

        return ResponseEntity.created(location).body(toEnrichedDTO(savedReview));
    }

    @Operation(summary = "(User) Update review")
    @PutMapping
    public ResponseEntity<ReviewDTO> updateReview(@RequestBody ReviewDTO reviewDTO) {
        Review updatedReview = reviewService.updateReview(reviewDTO);
        return ResponseEntity.ok().body(toEnrichedDTO(updatedReview));
    }

    @Operation(summary = "(Admin, User) Delete review by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ReviewDTO> deleteReviewById(@PathVariable Long id) {
        Review deletedReview = reviewService.deleteReview(id);
        return ResponseEntity.ok().body(toEnrichedDTO(deletedReview));
    }

    // ==========================================
    // ASSEMBLER AND ENRICHMENT HELPER METHODS
    // ==========================================

    /**
     * Converts a single Review entity to DTO and enriches its creator's connection.
     * Uses a dummy UserDTO to leverage the existing connection service.
     */
    private ReviewDTO toEnrichedDTO(Review review) {
        ReviewDTO dto = new ReviewDTO(review);
        if (dto.getCreatorUsername() != null) {
            UserDTO dummyUser = new UserDTO();
            dummyUser.setUsername(dto.getCreatorUsername());
            userConnectionService.enrichWithConnection(dummyUser);
            dto.setCreatorConnection(dummyUser.getConnection());
        }
        return dto;
    }

    /**
     * Converts a collection of Review entities to DTOs and enriches creators in batch.
     * Groups unique usernames to avoid redundant Mongo queries.
     */
    private List<ReviewDTO> toEnrichedDTOList(Collection<Review> reviews) {
        List<ReviewDTO> dtos = reviews.stream().map(ReviewDTO::new).toList();

        // 1. Extract unique usernames and create dummy UserDTOs mapping
        Map<String, UserDTO> uniqueUsersMap = new HashMap<>();
        for (ReviewDTO dto : dtos) {
            String username = dto.getCreatorUsername();
            if (username != null && !uniqueUsersMap.containsKey(username)) {
                UserDTO dummy = new UserDTO();
                dummy.setUsername(username);
                uniqueUsersMap.put(username, dummy);
            }
        }

        // 2. Enrich all unique dummy users in a single batch call to Mongo
        userConnectionService.enrichWithConnections(new ArrayList<>(uniqueUsersMap.values()));

        // 3. Assign the enriched connections back to the review DTOs
        for (ReviewDTO dto : dtos) {
            if (dto.getCreatorUsername() != null) {
                UserDTO enrichedDummy = uniqueUsersMap.get(dto.getCreatorUsername());
                if (enrichedDummy != null) {
                    dto.setCreatorConnection(enrichedDummy.getConnection());
                }
            }
        }

        return dtos;
    }

    /**
     * Converts a page of Review entities to a paginated response of enriched DTOs.
     */
    private PageResponse<ReviewDTO> toEnrichedPageResponse(Page<Review> reviews) {
        // 1. Map the pure entity page to a DTO page
        Page<ReviewDTO> dtoPage = reviews.map(ReviewDTO::new);

        // 2. Extract unique usernames from the current page and create dummy UserDTOs mapping
        Map<String, UserDTO> uniqueUsersMap = new HashMap<>();
        for (ReviewDTO dto : dtoPage.getContent()) {
            String username = dto.getCreatorUsername();
            if (username != null && !uniqueUsersMap.containsKey(username)) {
                UserDTO dummy = new UserDTO();
                dummy.setUsername(username);
                uniqueUsersMap.put(username, dummy);
            }
        }

        // 3. Enrich all unique dummy users in a single batch query
        userConnectionService.enrichWithConnections(new ArrayList<>(uniqueUsersMap.values()));

        // 4. Assign the enriched connections back to the review DTOs
        for (ReviewDTO dto : dtoPage.getContent()) {
            if (dto.getCreatorUsername() != null) {
                UserDTO enrichedDummy = uniqueUsersMap.get(dto.getCreatorUsername());
                if (enrichedDummy != null) {
                    dto.setCreatorConnection(enrichedDummy.getConnection());
                }
            }
        }

        // 5. Return the formatted PageResponse
        return PageFormatter.toPageResponse(dtoPage, dto -> dto);
    }
}
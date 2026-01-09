package com.tfg.backend.unit;

import com.tfg.backend.model.Review;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.ReviewRepository;
import com.tfg.backend.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ReviewServiceUTest {

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewService reviewService;


    // findById() method tests
    @Test
    void findById_ShouldReturnReview_WhenIdExists() {
        Long id = 1L;
        Review review = new Review();
        when(reviewRepository.findById(id)).thenReturn(Optional.of(review));

        Optional<Review> result = reviewService.findById(id);

        assertTrue(result.isPresent());
        assertEquals(review, result.get());
        verify(reviewRepository, times(1)).findById(id);
    }

    @Test
    void findById_ShouldReturnEmpty_WhenIdDoesNotExist() {
        Long id = 1L;
        when(reviewRepository.findById(id)).thenReturn(Optional.empty());

        Optional<Review> result = reviewService.findById(id);

        assertTrue(result.isEmpty());
        verify(reviewRepository, times(1)).findById(id);
    }


    // findAllByUser() method tests
    @Test
    void findAllByUser_ShouldReturnPage() {
        User user = new User();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Review> expectedPage = new PageImpl<>(Collections.singletonList(new Review()));

        when(reviewRepository.findAllByUser(user, pageable)).thenReturn(expectedPage);

        Page<Review> result = reviewService.findAllByUser(user, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(expectedPage, result);
        verify(reviewRepository, times(1)).findAllByUser(user, pageable);
    }


    // save() method tests
    @Test
    void save_ShouldReturnSavedReview() {
        Review review = new Review();
        when(reviewRepository.save(review)).thenReturn(review);

        Review result = reviewService.save(review);

        assertEquals(review, result);
        verify(reviewRepository, times(1)).save(review);
    }


    // deleteById() method tests
    @Test
    void deleteById_ShouldCallRepository() {
        Long id = 1L;

        reviewService.deleteById(id);

        verify(reviewRepository, times(1)).deleteById(id);
    }
}

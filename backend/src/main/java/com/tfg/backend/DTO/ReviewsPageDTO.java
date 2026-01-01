package com.tfg.backend.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReviewsPageDTO {
    private List<ReviewDTO> reviews;
    private Long totalReviews;
    private int currentPage;
    private int lastPage;
    private int pageSize;

    public ReviewsPageDTO(List<ReviewDTO> reviews, Long totalReviews, int currentPage, int lastPage, int pageSize) {
        this.reviews = reviews;
        this.totalReviews = totalReviews;
        this.currentPage = currentPage;
        this.lastPage = lastPage;
        this.pageSize = pageSize;
    }
}

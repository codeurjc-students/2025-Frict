package com.tfg.backend.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReviewsPageDTO {
    private List<ReviewDTO> reviews;
    private Long totalItems;
    private int currentPage;
    private int lastPage;
    private int pageSize;

    public ReviewsPageDTO(List<ReviewDTO> reviews, Long totalItems, int currentPage, int lastPage, int pageSize) {
        this.reviews = reviews;
        this.totalItems = totalItems;
        this.currentPage = currentPage;
        this.lastPage = lastPage;
        this.pageSize = pageSize;
    }
}

package com.tfg.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReviewStatsDTO {
    private long totalReviews;
    private double averageRating;
    private long star5;
    private long star4;
    private long star3;
    private long star2;
    private long star1;
    private double recommendationPercentage;
    private boolean userReviewed;
}

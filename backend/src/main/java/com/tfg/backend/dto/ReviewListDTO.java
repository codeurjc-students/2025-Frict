package com.tfg.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReviewListDTO {
    private List<ReviewDTO> reviews;

    public ReviewListDTO(List<ReviewDTO> reviews) {
        this.reviews = reviews;
    }
}

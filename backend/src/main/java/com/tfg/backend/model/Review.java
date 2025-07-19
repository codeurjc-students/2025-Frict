package com.tfg.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    private int rating;

    private String text;

    public Review() {}

    public Review(User user, Product product, int rating, String text) {
        this.user = user;
        this.product = product;
        this.rating = rating;
        this.text = text;
    }
}

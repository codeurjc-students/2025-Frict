package com.tfg.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "reviews")
@FilterDef(name = "activeProductReviewFilter")
@Filter(name = "activeProductReviewFilter", condition = "product_id IN (SELECT p.id FROM products p WHERE p.is_active = true)")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Product product;

    private int rating;

    @Column(columnDefinition = "LONGTEXT")
    private String text;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private boolean recommended;

    public Review() {}

    public Review(User user, Product product, int rating, String text, boolean recommended) {
        this.user = user;
        this.product = product;
        this.rating = rating;
        this.text = text;
        this.recommended = recommended;
    }
}

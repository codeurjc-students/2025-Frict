package com.tfg.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
//Inactive constraint, as NULL objects are not considered equal between each other
@Table(
        name = "order_items",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"order_id", "user_id", "product_id"}
        )
)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order = null; //Initially, this orderItem will belong to a user's cart

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private int quantity;

    @Column (nullable = false)
    private double totalPrice;

    public OrderItem() {
    }

    //Order items with null order means products that are already in assigned user's cart
    public OrderItem(Product product, User user, int quantity) {
        this.product = product;
        this.user = user;
        this.quantity = quantity;
        this.totalPrice = product.getCurrentPrice() * quantity;
    }
}

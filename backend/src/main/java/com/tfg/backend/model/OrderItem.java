package com.tfg.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
//Inactive constraint, as NULL objects are not considered equal between each other
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order = null; //Initially, this orderItem will belong to a user's cart

    //Necessary, as it is required by Cart Component to show the current product information
    //Always null when the item is part of an order
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    //Snapshot fields (used when the order is made, as the paid price is fixed at that moment)
    @Column(name = "product_name_snapshot")
    private String productName;

    @Column(name = "product_image_snapshot")
    private String productImageUrl;

    @Column(name = "product_price_snapshot")
    private double productPrice;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private int quantity;

    public OrderItem() {
    }

    //Order items with null order and snapshot fields means products that are already in assigned user's cart
    public OrderItem(Product product, User user, int quantity) {
        this.product = product;

        //Rubbish values until the order is placed (orderId not null)
        this.productName = product.getName();
        this.productImageUrl = product.getImages().getFirst().getImageUrl();
        this.productPrice = product.getCurrentPrice();

        this.user = user;
        this.quantity = quantity;
    }
}

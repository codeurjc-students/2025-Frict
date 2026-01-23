package com.tfg.backend.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.tfg.backend.utils.ReferenceNumberGenerator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @Setter(AccessLevel.NONE)
    private String referenceCode;

    private String name;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ProductImageInfo> images = new ArrayList<>();

    @Column (length = 5000)
    private String description;

    private double previousPrice = 0.00; // when updating products

    private double currentPrice;

    private boolean active = true;

    //It is NOT a column in Product table, and Hibernate automatically calculates its value
    @Formula("(SELECT COALESCE(SUM(oi.quantity), 0) FROM order_items oi WHERE oi.product_id = id AND oi.order_id IS NULL)")
    @Setter(AccessLevel.NONE)
    private int reservedUnits; //Contains the exact number of items with this product id that are in all users carts

    @ManyToMany(fetch = FetchType.EAGER)
    private List<Category> categories = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Review> reviews = new HashSet<>();

    @ManyToMany(mappedBy = "favouriteProducts")
    private Set<User> usersAsFavourite = new HashSet<>();

    //Controlled by the intermediate entities OrderItem and ShopStock
    //Only in cart items are deleted with product deletion
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShopStock> shopsStock = new ArrayList<>();

    public Product() {
    }

    public Product(String name, String description, double price) {
        this.referenceCode = ReferenceNumberGenerator.generateProductReferenceNumber();
        this.name = name;
        this.description = description;
        this.currentPrice = price;
    }
}

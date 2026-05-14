package com.tfg.backend.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.tfg.backend.utils.GlobalDefaults;
import com.tfg.backend.utils.ReferenceNumberGenerator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "products")
@FilterDef(name = "activeProductFilter")
@Filter(name = "activeProductFilter", condition = "is_active = true")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String referenceCode;

    private String name;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ProductImageInfo> images = new ArrayList<>();

    @Column (length = 5000)
    private String description;

    private double supplyPrice;

    private double previousPrice = 0.00; // when updating products

    private double currentPrice;

    @Column(name = "is_active")
    private boolean active = true;

    @ManyToMany(fetch = FetchType.EAGER)
    private List<Category> categories = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Review> reviews = new HashSet<>();

    @ManyToMany(mappedBy = "favouriteProducts")
    private Set<User> usersAsFavourite = new HashSet<>();

    //Controlled by the intermediate entities OrderItem and ShopStock
    //Only in cart items are deleted with product deletion
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShopStock> shopsStock = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductSpec> specifications = new ArrayList<>();

    //In-memory only field: Allows passing the selected local stock calculations back to ProductDTO entities
    @Transient
    private Integer availableUnits;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private LocalDateTime createdAt;

    public Product() {
    }

    public Product(String name, String description, double price, double supplyPrice) {
        this.referenceCode = ReferenceNumberGenerator.generateProductReferenceNumber();
        this.name = name;
        this.description = description;
        this.currentPrice = price;
        this.supplyPrice = supplyPrice;
    }

    @PrePersist
    protected void onCreate() {
        if (this.images == null || this.images.isEmpty()) {
            if (this.images == null) {
                this.images = new ArrayList<>();
            }
            this.images.add(new ProductImageInfo(GlobalDefaults.getDefaultProductImage(), this));
        }
    }
}

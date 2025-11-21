package com.tfg.backend.model;

import com.tfg.backend.utils.ImageUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Blob;
import java.util.HashSet;
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
    private String referenceCode;

    private String name;

    @Lob
    @Column(nullable = false)
    private Blob productImage = ImageUtils.prepareDefaultImage(Product.class);

    @Column (length = 5000)
    private String description;

    private double previousPrice = 0.00; // when updating products

    private double currentPrice;

    private boolean active = true;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Category> categories = new HashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Review> reviews = new HashSet<>();

    @ManyToMany(mappedBy = "productsInCart")
    private Set<User> usersAsInCart = new HashSet<>();

    @ManyToMany(mappedBy = "favouriteProducts")
    private Set<User> usersAsFavourite = new HashSet<>();

    @ManyToMany(mappedBy = "products")
    private Set<Order> orders = new HashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ShopStock> shopsStock = new HashSet<>();

    public Product() {
    }

    public Product(String referenceCode, String name, Blob productImage, String description, double price) {
        this.referenceCode = referenceCode;
        this.name = name;
        if (productImage !=null){
            this.productImage = productImage;
        }
        this.description = description;
        this.currentPrice = price;
    }
}

package com.tfg.backend.model;

import com.tfg.backend.utils.ImageUtils;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;

import java.sql.Blob;
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
    //Not Cascade.ALL, as it deletes all previous user's orders registries too
    @OneToMany(mappedBy = "product")
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShopStock> shopsStock = new ArrayList<>();

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

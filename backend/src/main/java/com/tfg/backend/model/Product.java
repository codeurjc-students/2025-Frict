package com.tfg.backend.model;

import com.tfg.backend.utils.PhotoUtils;
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
    private Blob photo;

    @Column (length = 5000)
    private String description;

    private double price;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Review> reviews = new HashSet<>();

    @ManyToMany(mappedBy = "products")
    private Set<Order> orders = new HashSet<>();

    @ManyToMany
    private Set<Shop> shopsWithStock = new HashSet<>();

    public Product() {
    }

    public Product(String referenceCode, String name, Blob photo, String description, double price) {
        this.id = id;
        this.referenceCode = referenceCode;
        this.name = name;
        if (photo !=null){
            this.photo = photo;
        } else {
            this.photo = PhotoUtils.setDefaultPhoto(Product.class);
        }
        this.description = description;
        this.price = price;
    }
}

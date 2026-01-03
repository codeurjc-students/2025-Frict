package com.tfg.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;

    private String bannerText;

    private String shortDescription;

    private String longDescription;

    @Embedded
    private ImageInfo categoryImage;

    @ManyToMany(mappedBy = "categories")
    private Set<Product> products = new HashSet<>();

    public Category() {
    }

    public Category(String name, String bannerText, String shortDescription, String longDescription) {
        this.name = name;
        this.bannerText = bannerText;
        this.shortDescription = shortDescription;
        this.longDescription = longDescription;
    }
}

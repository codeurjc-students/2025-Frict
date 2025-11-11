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
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;

    @Lob
    @Column(nullable = false)
    private Blob categoryImage = ImageUtils.prepareDefaultImage(Category.class);

    @ManyToMany(mappedBy = "categories")
    private Set<Product> products = new HashSet<>();

    public Category() {
    }

    public Category(String name, Blob image) {
        this.name = name;
        if (image != null){
            this.categoryImage = image;
        }
    }
}

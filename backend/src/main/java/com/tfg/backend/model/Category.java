package com.tfg.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tfg.backend.utils.GlobalDefaults;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    private String icon;

    private String bannerText;

    private String shortDescription;

    private String longDescription;

    @Embedded
    private ImageInfo categoryImage = GlobalDefaults.CATEGORY_IMAGE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnore // Avoids recursive calls while building the DTO objects
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Category> children = new ArrayList<>();

    @ManyToMany(mappedBy = "categories")
    private Set<Product> products = new HashSet<>();

    public Category() {
    }

    public Category(String name, String icon, String bannerText, String shortDescription, String longDescription) {
        this.name = name;
        this.bannerText = bannerText;
        this.icon = icon;
        this.shortDescription = shortDescription;
        this.longDescription = longDescription;
    }

    public void addChild(Category child) {
        children.add(child);
        child.setParent(this);
    }

    public void removeChild(Category child) {
        children.remove(child);
        child.setParent(null);
    }

    //Count products (sum of products linked with this category or its children)
    public int getProductsCount() {
        return collectUniqueProducts(new HashSet<>()).size();
    }

    private Set<Product> collectUniqueProducts(Set<Product> allProducts) {
        if (this.products != null) {
            allProducts.addAll(this.products);
        }

        if (this.children != null) {
            for (Category child : children) {
                child.collectUniqueProducts(allProducts);
            }
        }

        return allProducts;
    }
}

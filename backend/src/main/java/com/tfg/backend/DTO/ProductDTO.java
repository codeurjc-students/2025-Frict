package com.tfg.backend.DTO;

import com.tfg.backend.model.Category;
import com.tfg.backend.model.Product;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class ProductDTO {
    private Long id;
    private String referenceCode;
    private String name;
    private String description;
    private double price;
    private Set<Long> categoriesId = new HashSet<>();

    public ProductDTO() {
    }

    public ProductDTO(Product p) {
        this.id = p.getId();
        this.name = p.getName();
        this.referenceCode = p.getReferenceCode();
        this.description = p.getDescription();
        this.price = p.getPrice();
        //Converts all Category objets set into all categories id set
        this.categoriesId = p.getCategories().stream().map(Category::getId).collect(java.util.stream.Collectors.toSet());
    }

    public ProductDTO(Long id, String name, String referenceCode, String description, double price, Set<Category> categories) {
        this.id = id;
        this.name = name;
        this.referenceCode = referenceCode;
        this.description = description;
        this.price = price;
        this.categoriesId = categories.stream().map(Category::getId).collect(java.util.stream.Collectors.toSet());
    }

}

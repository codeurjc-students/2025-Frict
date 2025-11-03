package com.tfg.backend.DTO;

import com.tfg.backend.model.Category;
import com.tfg.backend.model.Product;
import lombok.Getter;
import lombok.Setter;
import java.util.Set;

@Getter
@Setter
public class ProductDTO {
    private Long id;
    private String referenceCode;
    private String name;
    private String imageUrl;
    private String description;
    private double previousPrice;
    private double currentPrice;
    private Set<Long> categoriesId;

    public ProductDTO(Product p) {
        this.id = p.getId();
        this.name = p.getName();
        this.referenceCode = p.getReferenceCode();
        this.imageUrl = "/api/products/img/" + id;
        this.description = p.getDescription();
        this.previousPrice = p.getPreviousPrice();
        this.currentPrice = p.getCurrentPrice();
        this.categoriesId = p.getCategories().stream().map(Category::getId).collect(java.util.stream.Collectors.toSet());
    }
}

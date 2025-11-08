package com.tfg.backend.DTO;

import com.tfg.backend.model.Category;
import com.tfg.backend.model.Product;
import lombok.Getter;
import lombok.Setter;
import java.util.Set;

//More fields than the original product, as they are calculated and sent from other parts of the backend
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
    private String discount;
    private int availableUnits = 0; //Filled outside the constructor (toProductsPageDTO method)
    private Set<Long> categoriesId;

    public ProductDTO(Product p) {
        this.id = p.getId();
        this.name = p.getName();
        this.referenceCode = p.getReferenceCode();
        this.imageUrl = "/api/v1/products/image/" + id;
        this.description = p.getDescription();
        this.previousPrice = p.getPreviousPrice();
        this.currentPrice = p.getCurrentPrice();
        if (previousPrice != 0.0 && currentPrice < previousPrice){
            this.discount = "-" + String.valueOf((int) Math.floor(((this.previousPrice - this.currentPrice) / this.previousPrice) * 100)) + "%";
        }
        else this.discount = "0%";
        this.categoriesId = p.getCategories().stream().map(Category::getId).collect(java.util.stream.Collectors.toSet());
    }
}

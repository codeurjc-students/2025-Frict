package com.tfg.backend.DTO;

import com.tfg.backend.model.Product;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductDTO {
    private Long id;
    private String referenceCode;
    private String name;
    private String description;
    private double price;

    public ProductDTO() {
    }

    public ProductDTO(Product p) {
        this.id = p.getId();
        this.name = p.getName();
        this.referenceCode = p.getReferenceCode();
        this.description = p.getDescription();
        this.price = p.getPrice();
    }

    public ProductDTO(Long id, String name, String referenceCode, String description, double price) {
        this.id = id;
        this.name = name;
        this.referenceCode = referenceCode;
        this.description = description;
        this.price = price;
    }

}

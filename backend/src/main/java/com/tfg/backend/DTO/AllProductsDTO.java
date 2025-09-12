package com.tfg.backend.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AllProductsDTO {
    private List<ProductDTO> products;

    public AllProductsDTO(List<ProductDTO> products) {
        this.products = products;
    }
}

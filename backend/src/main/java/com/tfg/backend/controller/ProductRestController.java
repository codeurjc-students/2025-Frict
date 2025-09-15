package com.tfg.backend.controller;

import com.tfg.backend.DTO.AllProductsDTO;
import com.tfg.backend.DTO.ProductDTO;
import com.tfg.backend.model.Product;
import com.tfg.backend.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
public class ProductRestController {

    @Autowired
    private ProductService productService;

    @GetMapping("/all")
    public ResponseEntity<AllProductsDTO> showAllProducts() {
        List<Product> products = productService.findAll();
        List<ProductDTO> dtos = new ArrayList<>();
        for (Product p : products) {
            dtos.add(new ProductDTO(p));
        }
        return ResponseEntity.ok(new AllProductsDTO(dtos));
    }
}

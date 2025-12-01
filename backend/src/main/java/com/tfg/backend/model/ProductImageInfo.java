package com.tfg.backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ProductImageInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String imageUrl; // http://localhost:9000/images/products/foto.jpg
    private String s3Key;    // products/foto.jpg (Ruta relativa dentro del bucket)
    private String fileName; // archivo_original.jpg

    @ManyToOne
    @JoinColumn(name = "product_id")
    @JsonBackReference
    private Product product;

    public ProductImageInfo() {
    }

    public ProductImageInfo(String imageUrl, String s3Key, String fileName, Product product) {
        this.imageUrl = imageUrl;
        this.s3Key = s3Key;
        this.fileName = fileName;
        this.product = product;
    }
}

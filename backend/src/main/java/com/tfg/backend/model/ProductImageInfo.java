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
    private String s3Key;    // products/foto.jpg (relative route inside the bucket)
    private String fileName; // archivo_original.jpg

    @ManyToOne
    @JoinColumn(name = "product_id")
    @JsonBackReference
    private Product product;

    public ProductImageInfo() {
    }

    public ProductImageInfo(ImageInfo info, Product product) {
        this.imageUrl = info.getImageUrl();
        this.s3Key = info.getS3Key();
        this.fileName = info.getFileName();
        this.product = product;
    }
}

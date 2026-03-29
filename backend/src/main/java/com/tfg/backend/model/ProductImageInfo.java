package com.tfg.backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Objects;

@Entity
@Getter
@Setter
public class ProductImageInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded // Insert the 3 ImageInfo fields in this DB table
    private ImageInfo imageInfo = new ImageInfo();

    @ManyToOne
    @JoinColumn(name = "product_id")
    @JsonBackReference
    private Product product;

    public ProductImageInfo() {
    }

    public ProductImageInfo(ImageInfo imageInfo, Product product) {
        this.imageInfo = imageInfo;
        this.product = product;
    }

    // Un equals seguro para Entidades basado en la clave de negocio (S3 Key)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductImageInfo that = (ProductImageInfo) o;
        return imageInfo != null && that.imageInfo != null &&
                Objects.equals(imageInfo.getS3Key(), that.imageInfo.getS3Key());
    }

    @Override
    public int hashCode() {
        return imageInfo != null && imageInfo.getS3Key() != null ? imageInfo.getS3Key().hashCode() : 31;
    }

    // Métodos delegados (Opcional, para no romper tu código actual)
    public String getImageUrl() {
        return imageInfo != null ? imageInfo.getImageUrl() : null;
    }

    public String getS3Key() {
        return imageInfo != null ? imageInfo.getS3Key() : null;
    }

    public String getFileName() {
        return imageInfo != null ? imageInfo.getFileName() : null;
    }
}
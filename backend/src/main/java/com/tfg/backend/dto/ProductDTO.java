package com.tfg.backend.dto;

import com.tfg.backend.model.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//More fields than the original product, as they are calculated and sent from other parts of the backend
@Getter
@Setter
public class ProductDTO {
    private Long id;
    private String referenceCode;
    private String name;
    private List<ImageInfo> imagesInfo = new ArrayList<>();
    private String description;
    private double previousPrice;
    private double currentPrice;
    private boolean active;
    private String discount;
    private List<CategoryDTO> categories = new ArrayList<>();
    private int totalUnits;
    private int shopsWithStock;
    private double averageRating;
    private int totalReviews;

    public ProductDTO() { //Used by Spring to achieve conversion between JSON responses that do not include all fields
    }

    public ProductDTO(Product p) {
        this.id = p.getId();
        this.name = p.getName();
        this.referenceCode = p.getReferenceCode();
        for (ProductImageInfo image : p.getImages()) {
            this.imagesInfo.add(new ImageInfo(image.getImageUrl(), image.getS3Key(), image.getFileName()));
        }
        this.description = p.getDescription();
        this.previousPrice = p.getPreviousPrice();
        this.currentPrice = p.getCurrentPrice();
        this.active = p.isActive();
        if (previousPrice != 0.0 && currentPrice < previousPrice){
            this.discount = "-" + String.valueOf((int) Math.floor(((this.previousPrice - this.currentPrice) / this.previousPrice) * 100)) + "%";
        }
        else this.discount = "0%";

        List<CategoryDTO> dtos = new ArrayList<>();
        for (Category c : p.getCategories()) {
            dtos.add(new CategoryDTO(c));
        }
        this.categories = dtos;

        int totalUnits = 0;
        for (ShopStock s : p.getShopsStock()) {
            totalUnits += s.getUnits();
        }
        this.totalUnits = totalUnits;
        this.shopsWithStock = p.getShopsStock().size();

        //Total reviews and average rating
        double totalRating = 0.0;
        Set<Review> reviews = p.getReviews();
        this.totalReviews = reviews.size();
        if (!reviews.isEmpty()){
            for (Review r : reviews) {
                totalRating += r.getRating();
            }
            this.averageRating = totalRating / reviews.size();
        }
        else this.averageRating = totalRating;
    }
}

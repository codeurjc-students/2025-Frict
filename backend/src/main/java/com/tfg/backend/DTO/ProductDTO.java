package com.tfg.backend.DTO;

import com.tfg.backend.model.Category;
import com.tfg.backend.model.Product;
import com.tfg.backend.model.Review;
import com.tfg.backend.model.ShopStock;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
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
    private Set<Long> categoriesId = new HashSet<>();
    private int availableUnits;
    private double averageRating;
    private int totalReviews;

    public ProductDTO() { //Used by Spring to achieve conversion between JSON responses that do not include all fields
    }

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

        //Available units
        int totalUnits = 0;
        for (ShopStock s : p.getShopsStock()) {
            totalUnits += s.getStock();
        }
        this.availableUnits = totalUnits;

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

package com.tfg.backend.DTO;

import com.tfg.backend.model.Category;
import com.tfg.backend.model.Product;
import com.tfg.backend.model.Review;
import com.tfg.backend.model.ShopStock;
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
    private String imageUrl;
    private String thumbnailUrl;
    private String description;
    private double previousPrice;
    private double currentPrice;
    private String discount;
    private List<CategoryDTO> categories = new ArrayList<>();
    private int availableUnits; //Available units will be all units stock which are not in any user cart yet
    private double averageRating;
    private int totalReviews;

    public ProductDTO() { //Used by Spring to achieve conversion between JSON responses that do not include all fields
    }

    public ProductDTO(Product p) {
        this.id = p.getId();
        this.name = p.getName();
        this.referenceCode = p.getReferenceCode();
        this.imageUrl = "/api/v1/products/image/" + id;
        this.thumbnailUrl = "/api/v1/products/thumbnail/" + id;
        this.description = p.getDescription();
        this.previousPrice = p.getPreviousPrice();
        this.currentPrice = p.getCurrentPrice();
        if (previousPrice != 0.0 && currentPrice < previousPrice){
            this.discount = "-" + String.valueOf((int) Math.floor(((this.previousPrice - this.currentPrice) / this.previousPrice) * 100)) + "%";
        }
        else this.discount = "0%";

        List<CategoryDTO> dtos = new ArrayList<>();
        for (Category c : p.getCategories()) {
            dtos.add(new CategoryDTO(c));
        }
        this.categories = dtos;

        //Available units (total - reserved)
        int totalUnits = 0;
        for (ShopStock s : p.getShopsStock()) {
            totalUnits += s.getStock();
        }
        this.availableUnits = totalUnits - p.getReservedUnits();

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

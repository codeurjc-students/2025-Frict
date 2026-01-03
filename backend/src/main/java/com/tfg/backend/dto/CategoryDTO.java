package com.tfg.backend.dto;

import com.tfg.backend.model.Category;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryDTO {
    private Long id;
    private String name;
    private String bannerText;
    private String shortDescription;
    private String longDescription;
    private String imageUrl;

    public CategoryDTO() {
    }

    public CategoryDTO(Category c) {
        this.id = c.getId();
        this.name = c.getName();
        this.bannerText = c.getBannerText();
        this.shortDescription = c.getShortDescription();
        this.longDescription = c.getLongDescription();
        this.imageUrl = c.getCategoryImage().getImageUrl();
    }
}

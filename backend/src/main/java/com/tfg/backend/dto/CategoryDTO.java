package com.tfg.backend.dto;

import com.tfg.backend.model.Category;
import com.tfg.backend.model.ImageInfo;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CategoryDTO {
    private Long id;
    private String name;
    private String icon;
    private String bannerText;
    private String shortDescription;
    private String longDescription;
    private ImageInfo imageInfo;
    private int timesUsed;
    private Long parentId;
    private List<CategoryDTO> children = new ArrayList<>();

    public CategoryDTO() {
    }

    public CategoryDTO(Category c) {
        this.id = c.getId();
        this.name = c.getName();
        this.icon = c.getIcon();
        this.bannerText = c.getBannerText();
        this.shortDescription = c.getShortDescription();
        this.longDescription = c.getLongDescription();

        if (c.getCategoryImage() != null) {
            this.imageInfo = c.getCategoryImage();
        }

        this.timesUsed = c.getTimesUsed();

        if(c.getParent() != null){
            this.parentId = c.getParent().getId();
        }

        //Recursive category children instantiation
        if (c.getChildren() != null && !c.getChildren().isEmpty()) {
            this.children = c.getChildren().stream()
                    .map(CategoryDTO::new) // Recursive call
                    .toList();
        }
    }
}

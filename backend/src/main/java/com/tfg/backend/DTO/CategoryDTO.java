package com.tfg.backend.DTO;

import com.tfg.backend.model.Category;
import com.tfg.backend.model.Product;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryDTO {
    private Long id;
    private String name;
    private String imageUrl;

    public CategoryDTO() {
    }

    public CategoryDTO(Category c) {
        this.id = c.getId();
        this.name = c.getName();
        this.imageUrl = c.getCategoryImage().getImageUrl();
    }
}

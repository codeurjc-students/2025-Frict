package com.tfg.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CategoryListDTO {
    private List<CategoryDTO> categories;

    public CategoryListDTO(List<CategoryDTO> categories) {
        this.categories = categories;
    }
}

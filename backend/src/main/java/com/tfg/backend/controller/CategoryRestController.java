package com.tfg.backend.controller;

import com.tfg.backend.DTO.CategoryDTO;
import com.tfg.backend.model.Category;
import com.tfg.backend.model.Product;
import com.tfg.backend.service.CategoryService;
import com.tfg.backend.utils.ImageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Blob;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryRestController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/")
    public ResponseEntity<CategoryDTO> getCategoryByName(@RequestParam("name") String name) {
        Optional<Category> category = categoryService.findByName(name);
        if (!category.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new CategoryDTO(category.get()));
    }

    @GetMapping("/image/{id}")
    public ResponseEntity<byte[]> showCategoryImage(@PathVariable long id) {
        Optional<Category> categoryOptional = categoryService.findById(id);
        if (!categoryOptional.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Category category = categoryOptional.get();
        return ImageUtils.serveImage(category.getCategoryImage());
    }


    @PutMapping("/image/{id}")
    public ResponseEntity<String> updateCategoryImage(@PathVariable Long id, @RequestPart("image") MultipartFile image) {
        Optional<Category> categoryOptional = categoryService.findById(id);
        if (categoryOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Category category = categoryOptional.get();

        Blob categoryImage = ImageUtils.prepareImage(image);
        category.setCategoryImage(categoryImage);
        categoryService.save(category);
        return ResponseEntity.ok().build();
    }
}

package com.tfg.backend.controller;

import com.tfg.backend.dto.CategoryDTO;
import com.tfg.backend.dto.CategoryListDTO;
import com.tfg.backend.model.Category;
import com.tfg.backend.model.ImageInfo;
import com.tfg.backend.service.CategoryService;
import com.tfg.backend.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryRestController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private StorageService storageService;

    @GetMapping("/")
    public ResponseEntity<CategoryListDTO> showAllCategories() {
        List<CategoryDTO> dtos = categoryService.findAll().stream().map(CategoryDTO::new).toList();
        return ResponseEntity.ok(new CategoryListDTO(dtos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long id) {
        return categoryService.findById(id)
                .map(CategoryDTO::new)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category with ID " + id + " not found."));
    }

    @PostMapping(value = "/{id}/image")
    public ResponseEntity<Category> uploadCategoryImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {

        // A. Find category
        Optional<Category> categoryOptional = categoryService.findById(id);
        if(categoryOptional.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category with ID " + id + " not found.");
        }
        Category category = categoryOptional.get();

        // B. Previous cleaning: If there is already a photo, delete the one from MinIO
        if (category.getCategoryImage() != null && category.getCategoryImage().getS3Key() != null) {
            storageService.deleteFile(category.getCategoryImage().getS3Key());
        }

        // C. Upload to "categories" folder
        Map<String, String> res = storageService.uploadFile(file, "categories");

        // D. Create ImageInfo object
        ImageInfo imageInfo = new ImageInfo(
                res.get("url"),
                res.get("key"),
                file.getOriginalFilename()
        );

        // E. Save
        category.setCategoryImage(imageInfo);

        return ResponseEntity.ok(categoryService.save(category));
    }

    @DeleteMapping("/{id}/image")
    public ResponseEntity<Category> deleteCategoryImage(@PathVariable Long id) {

        //A. Find category
        Optional<Category> categoryOptional = categoryService.findById(id);
        if(categoryOptional.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category with ID " + id + " not found.");
        }
        Category category = categoryOptional.get();

        // B. Check if there is something to delete
        if (category.getCategoryImage() != null) {
            storageService.deleteFile(category.getCategoryImage().getS3Key());

            // 2. Set embedded field to null -> JPA automatically updates image_url, s3_key,... colums to null in DB
            category.setCategoryImage(null);

            // 3. Save changes
            return ResponseEntity.ok(categoryService.save(category));
        }

        // If there were no image, return the original category
        return ResponseEntity.ok(category);
    }
}

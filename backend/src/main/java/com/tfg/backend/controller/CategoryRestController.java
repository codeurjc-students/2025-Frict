package com.tfg.backend.controller;

import com.tfg.backend.dto.CategoryDTO;
import com.tfg.backend.dto.ListResponse;
import com.tfg.backend.model.Category;
import com.tfg.backend.model.ImageInfo;
import com.tfg.backend.model.User;
import com.tfg.backend.service.CategoryService;
import com.tfg.backend.service.StorageService;
import com.tfg.backend.utils.GlobalDefaults;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Category Management", description = "Product categories data management")
public class CategoryRestController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private StorageService storageService;


    @Operation(summary = "(All) Get all categories (listed)")
    @GetMapping("/")
    public ResponseEntity<ListResponse<CategoryDTO>> showAllCategories() {
        List<CategoryDTO> dtos = categoryService.findAll().stream().map(CategoryDTO::new).toList();
        return ResponseEntity.ok(new ListResponse<>(dtos));
    }


    @Operation(summary = "(All) Get category by ID")
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long id) {
        Category category = findCategoryHelper(id);
        return ResponseEntity.ok(new CategoryDTO(category));
    }


    @Operation(summary = "(Admin) Update remote category image")
    @PostMapping(value = "/{id}/image")
    public ResponseEntity<Category> uploadCategoryImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {

        Category category = findCategoryHelper(id);

        // Delete the previous image from MinIO if it is not the default photo
        if (category.getCategoryImage() != null && !category.getCategoryImage().equals(GlobalDefaults.CATEGORY_IMAGE)){
            storageService.deleteFile(category.getCategoryImage().getS3Key());
        }

        Map<String, String> res = storageService.uploadFile(file, "categories");

        ImageInfo imageInfo = new ImageInfo(
                res.get("url"),
                res.get("key"),
                file.getOriginalFilename()
        );

        category.setCategoryImage(imageInfo);
        return ResponseEntity.ok(categoryService.save(category));
    }


    @Operation(summary = "(Admin) Delete remote category image")
    @DeleteMapping("/{id}/image")
    public ResponseEntity<Category> deleteCategoryImage(@PathVariable Long id) {
        Category category = findCategoryHelper(id);

        if (!category.getCategoryImage().equals(GlobalDefaults.CATEGORY_IMAGE)){
            storageService.deleteFile(category.getCategoryImage().getS3Key());
            category.setCategoryImage(GlobalDefaults.CATEGORY_IMAGE);
            return ResponseEntity.ok(categoryService.save(category));
        }

        //Default category image -> Do not delete
        return ResponseEntity.ok(category);
    }

    private Category findCategoryHelper(Long id) {
        return this.categoryService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category with ID " + id + " does not exist."));
    }
}

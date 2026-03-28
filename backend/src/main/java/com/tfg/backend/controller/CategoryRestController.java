package com.tfg.backend.controller;

import com.tfg.backend.dto.CategoryDTO;
import com.tfg.backend.dto.PageResponse;
import com.tfg.backend.model.Category;
import com.tfg.backend.service.CategoryService;
import com.tfg.backend.utils.PageFormatter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Category Management", description = "Product categories data management")
@RequiredArgsConstructor
public class CategoryRestController {

    private final CategoryService categoryService;


    @Operation(summary = "(All) Get all categories (paged)")
    @GetMapping("/")
    public ResponseEntity<PageResponse<CategoryDTO>> showAllCategoriesPage(Pageable pageable) {
        Page<Category> categories = categoryService.getCategoriesPage(pageable);
        return ResponseEntity.ok(PageFormatter.toPageResponse(categories, CategoryDTO::new));
    }


    @Operation(summary = "(All) Get all categories (listed)")
    @GetMapping("/list")
    public ResponseEntity<List<CategoryDTO>> showAllCategories() {
        List<CategoryDTO> dtos = categoryService.findAll().stream().map(CategoryDTO::new).toList();
        return ResponseEntity.ok(dtos);
    }


    @Operation(summary = "(Admin) Create category")
    @PostMapping
    public ResponseEntity<CategoryDTO> createCategory(@RequestBody CategoryDTO categoryDTO) {
        Category savedNewCategory = categoryService.createCategory(categoryDTO);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedNewCategory.getId())
                .toUri();

        return ResponseEntity.created(location).body(new CategoryDTO(savedNewCategory));
    }


    @Operation(summary = "(Admin) Update category by ID")
    @PutMapping("/{id}")
    public ResponseEntity<CategoryDTO> updateCategory(@PathVariable Long id, @RequestBody CategoryDTO categoryDTO) {
        Category savedUpdatedCategory = categoryService.updateCategory(id, categoryDTO);
        return ResponseEntity.ok(new CategoryDTO(savedUpdatedCategory));
    }


    @Operation(summary = "(Admin) Delete category by ID (Recursive)")
    @DeleteMapping("/{id}")
    public ResponseEntity<CategoryDTO> deleteCategory(@PathVariable Long id) {
        Category deletedCategory = categoryService.deleteCategory(id);
        return ResponseEntity.ok(new CategoryDTO(deletedCategory));
    }


    @Operation(summary = "(All) Get category by ID")
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long id) {
        Category category = categoryService.findByIdHelper(id);
        return ResponseEntity.ok(new CategoryDTO(category));
    }


    @Operation(summary = "(All) Get category by name")
    @GetMapping("/name/{name}")
    public ResponseEntity<CategoryDTO> getCategoryByName(@PathVariable String name) {
        Category category = categoryService.findByNameHelper(name);
        return ResponseEntity.ok(new CategoryDTO(category));
    }


    @Operation(summary = "(Admin) Update remote category image")
    @PostMapping(value = "/{id}/image")
    public ResponseEntity<CategoryDTO> uploadCategoryImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile file) {
        Category category = categoryService.uploadCategoryImage(id, file);
        return ResponseEntity.ok(new CategoryDTO(category));
    }


    @Operation(summary = "(Admin) Delete remote category image")
    @DeleteMapping("/{id}/image")
    public ResponseEntity<CategoryDTO> deleteCategoryImage(@PathVariable Long id) {
        Category category = categoryService.deleteCategoryImage(id);
        return ResponseEntity.ok(new CategoryDTO(category));
    }
}

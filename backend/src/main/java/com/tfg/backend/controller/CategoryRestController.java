package com.tfg.backend.controller;

import com.tfg.backend.dto.*;
import com.tfg.backend.model.*;
import com.tfg.backend.service.CategoryService;
import com.tfg.backend.service.ProductService;
import com.tfg.backend.service.StorageService;
import com.tfg.backend.utils.GlobalDefaults;
import com.tfg.backend.utils.PageFormatter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@Tag(name = "Category Management", description = "Product categories data management")
public class CategoryRestController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private ProductService productService;

    @Operation(summary = "(All) Get all categories (paged)")
    @GetMapping("/")
    public ResponseEntity<PageResponse<CategoryDTO>> showAllCategoriesPage(Pageable pageable) {
        Page<Category> categories = categoryService.getCategoriesPage(pageable);
        return ResponseEntity.ok(PageFormatter.toPageResponse(categories, CategoryDTO::new));
    }


    @Operation(summary = "(All) Get all categories (listed)")
    @GetMapping("/list")
    public ResponseEntity<ListResponse<CategoryDTO>> showAllCategories() {
        List<CategoryDTO> dtos = categoryService.findAll().stream().map(CategoryDTO::new).toList();
        return ResponseEntity.ok(new ListResponse<>(dtos));
    }


    @Operation(summary = "(Admin) Create category")
    @PostMapping
    public ResponseEntity<CategoryDTO> createCategory(@RequestBody CategoryDTO categoryDTO) {
        Category othersCategory = categoryService.findByName("Otros")
                .orElseThrow(() -> new EntityNotFoundException("Category with name \"Otros\" does not exist."));

        if (othersCategory.getId().equals(categoryDTO.getId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot create a subcategory for \"Otros\" category");
        }

        Category newCategory = new Category(categoryDTO.getName(), categoryDTO.getIcon(), categoryDTO.getBannerText(), categoryDTO.getShortDescription(), categoryDTO.getLongDescription());

        Long parentId = categoryDTO.getParentId();
        if (parentId != null) {
            Category parentCategory = findCategoryHelper(parentId);
            parentCategory.addChild(newCategory);
        }

        Category savedNewCategory = categoryService.save(newCategory);

        return ResponseEntity.status(HttpStatus.CREATED).body(new CategoryDTO(savedNewCategory));
    }


    @Operation(summary = "(Admin) Update category by ID")
    @PutMapping("/{id}")
    public ResponseEntity<CategoryDTO> updateCategory(@PathVariable Long id, @RequestBody CategoryDTO categoryDTO) {
        Category category = findCategoryHelper(id);

        Category othersCategory = categoryService.findByName("Otros")
                .orElseThrow(() -> new EntityNotFoundException("Category with name \"Otros\" does not exist."));

        if (othersCategory.getId().equals(categoryDTO.getId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot update \"Otros\" category");
        }

        category.setName(categoryDTO.getName());
        category.setIcon(categoryDTO.getIcon());
        category.setBannerText(categoryDTO.getBannerText());
        category.setShortDescription(categoryDTO.getShortDescription());
        category.setLongDescription(categoryDTO.getLongDescription());

        Long newParentId = categoryDTO.getParentId();
        Category currentParent = category.getParent();

        if (newParentId == null) {
            // Move to root
            if (currentParent != null) {
                currentParent.removeChild(category);
            }
        } else {
            // Assign new parent
            boolean isSameParent = currentParent != null && currentParent.getId().equals(newParentId);

            if (!isSameParent) {
                // Retrieve the new parent
                Category newParent = findCategoryHelper(newParentId);
                validateCircularReference(category, newParent);

                if (newParent.getProducts() != null && !newParent.getProducts().isEmpty()) {

                    // Search for "Others" category
                    Category otrosCategory = categoryService.findByName("Otros")
                            .orElseThrow(() -> new EntityNotFoundException("Category with name \"Otros\" does not exist."));

                    List<Product> productsToClean = new ArrayList<>(newParent.getProducts());

                    for (Product product : productsToClean) {
                        // Remove parent
                        product.getCategories().remove(newParent);

                        // And if after removing the parent the product is unclassified (no categories), then assign "Others" category
                        if (product.getCategories().isEmpty()) {
                            product.getCategories().add(otrosCategory);
                        }
                        productService.update(product);
                    }

                    // Clean the list from memory
                    newParent.getProducts().clear();
                }

                if (currentParent != null) {
                    currentParent.removeChild(category);
                }
                newParent.addChild(category);
            }
        }

        Category savedCategory = categoryService.save(category);
        return ResponseEntity.ok(new CategoryDTO(savedCategory));
    }


    @Operation(summary = "(Admin) Delete category by ID (Recursive)")
    @DeleteMapping("/{id}")
    public ResponseEntity<CategoryDTO> deleteCategory(@PathVariable Long id) {
        Category categoryToDelete = findCategoryHelper(id);

        Category othersCategory = categoryService.findByName("Otros")
                .orElseThrow(() -> new EntityNotFoundException("Category 'Otros' not found"));

        if (othersCategory.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete 'Otros' category");
        }

        // Delete its children first
        categoryService.processCategoryForDeletion(categoryToDelete, othersCategory);

        // Unlink from parent (if any)
        if (categoryToDelete.getParent() != null) {
            categoryToDelete.getParent().removeChild(categoryToDelete);
        }

        // Final deletion
        categoryService.delete(categoryToDelete);
        return ResponseEntity.ok(new CategoryDTO(categoryToDelete));
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
            @RequestParam("image") MultipartFile file) throws IOException {

        Category category = findCategoryHelper(id);

        // Delete the previous image from MinIO if it is not the default photo
        if (category.getCategoryImage() != null && !category.getCategoryImage().equals(GlobalDefaults.CATEGORY_IMAGE)){
            storageService.deleteFile(category.getCategoryImage().getS3Key());
        }

        if (!file.isEmpty()){
            Map<String, String> res = storageService.uploadFile(file, "categories");
            ImageInfo imageInfo = new ImageInfo(
                    res.get("url"),
                    res.get("key"),
                    file.getOriginalFilename()
            );

            category.setCategoryImage(imageInfo);
        }
        else {
            category.setCategoryImage(GlobalDefaults.CATEGORY_IMAGE);
        }

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

    private void validateCircularReference(Category categoryToMove, Category newParentCandidate) {
        // Avoid recursive parent relations
        if (categoryToMove.getId().equals(newParentCandidate.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A category cannot be its own parent.");
        }

        // Avoid that any of its children can be its parent
        Category temp = newParentCandidate;
        while (temp.getParent() != null) {
            if (temp.getParent().getId().equals(categoryToMove.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot move category into its own descendant.");
            }
            temp = temp.getParent();
        }
    }
}

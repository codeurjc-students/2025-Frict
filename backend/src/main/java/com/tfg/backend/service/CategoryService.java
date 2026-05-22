package com.tfg.backend.service;

import com.tfg.backend.dto.CategoryDTO;
import com.tfg.backend.model.Category;
import com.tfg.backend.model.ImageInfo;
import com.tfg.backend.model.Product;
import com.tfg.backend.repository.CategoryRepository;
import com.tfg.backend.utils.GlobalDefaults;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryService {

    private final ImageService imageService;
    private final CategoryRepository categoryRepository;

    // --- Read-only methods ---

    public List<Category> findAll() {
        return categoryRepository.findRootsWithChildren();
    }

    public Page<Category> getCategoriesPage(Pageable pageable) {
        Page<Category> rootsPage = categoryRepository.findRoots(pageable);

        if (rootsPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Category> rootsWithChildren = categoryRepository.fetchChildrenFor(rootsPage.getContent());
        return new PageImpl<>(rootsWithChildren, pageable, rootsPage.getTotalElements());
    }

    public Optional<Category> findById(long id) {
        return categoryRepository.findByIdWithChildren(id);
    }

    public Optional<Category> findByName(String name) {
        return categoryRepository.findByNameWithChildren(name);
    }

    public Category findByIdHelper(Long id) {
        return this.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category with ID " + id + " does not exist."));
    }

    public Category findByNameHelper(String name) {
        return this.findByName(name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category with name " + name + " does not exist."));
    }

    // --- Writing  methods (override Transactional) ---

    @Transactional
    public Category createCategory(CategoryDTO dto) {
        Category othersCategory = this.findByNameHelper("Otros");

        if (othersCategory.getId().equals(dto.getId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot create a subcategory for \"Otros\" category");
        }

        Category newCategory = new Category(dto.getName(), dto.getIcon(), dto.getBannerText(), dto.getShortDescription(), dto.getLongDescription());

        Long parentId = dto.getParentId();
        if (parentId != null) {
            Category parentCategory = this.findByIdHelper(parentId);
            parentCategory.addChild(newCategory);
        }

        return categoryRepository.save(newCategory);
    }

    @Transactional
    public Category updateCategory(Long id, CategoryDTO dto) {
        Category category = this.findByIdHelper(id);
        Category othersCategory = this.findByNameHelper("Otros");

        if (othersCategory.getId().equals(id)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot update \"Otros\" category");
        }

        category.setName(dto.getName());
        category.setIcon(dto.getIcon());
        category.setBannerText(dto.getBannerText());
        category.setShortDescription(dto.getShortDescription());
        category.setLongDescription(dto.getLongDescription());

        Long newParentId = dto.getParentId();
        Category currentParent = category.getParent();

        if (newParentId == null) {
            if (currentParent != null) {
                currentParent.removeChild(category);
            }
        } else {
            boolean isSameParent = currentParent != null && currentParent.getId().equals(newParentId);

            if (!isSameParent) {
                Category newParent = this.findByIdHelper(newParentId);
                validateCircularReference(category, newParent);

                if (newParent.getProducts() != null && !newParent.getProducts().isEmpty()) {
                    List<Product> productsToClean = new ArrayList<>(newParent.getProducts());

                    for (Product product : productsToClean) {
                        product.getCategories().remove(newParent);
                        if (product.getCategories().isEmpty()) {
                            product.getCategories().add(othersCategory);
                        }
                        // Updated automatically
                    }
                    newParent.getProducts().clear();
                }

                if (currentParent != null) {
                    currentParent.removeChild(category);
                }
                newParent.addChild(category);
            }
        }

        return category; // Saved automatically
    }

    @Transactional
    public Category deleteCategory(Long id) {
        Category categoryToDelete = this.findByIdHelper(id);
        Category othersCategory = this.findByNameHelper("Otros");

        if (othersCategory.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete 'Otros' category");
        }

        this.processCategoryForDeletion(categoryToDelete, othersCategory);

        if (categoryToDelete.getParent() != null) {
            categoryToDelete.getParent().removeChild(categoryToDelete);
        }

        if(!GlobalDefaults.isDefaultCategoryImage(categoryToDelete.getCategoryImage())){
            imageService.deleteFile(categoryToDelete.getCategoryImage().getS3Key());
        }

        categoryRepository.delete(categoryToDelete);
        return categoryToDelete;
    }

    @Transactional
    public void processCategoryForDeletion(Category category, Category othersCategory) {
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            List<Category> children = new ArrayList<>(category.getChildren());
            for (Category child : children) {
                processCategoryForDeletion(child, othersCategory);
            }
        }

        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            List<Product> productsToUpdate = new ArrayList<>(category.getProducts());

            for (Product product : productsToUpdate) {
                product.getCategories().remove(category);

                if (product.getCategories().isEmpty()) {
                    product.getCategories().add(othersCategory);
                }
                // Updated automatically
            }
            category.getProducts().clear();
        }
    }

    @Transactional
    public Category uploadCategoryImage(Long id, MultipartFile file) {
        Category category = this.findByIdHelper(id);

        ImageInfo newImage = imageService.processImageReplacement(
                category.getCategoryImage(),
                file,
                "categories",
                GlobalDefaults::isDefaultCategoryImage,
                GlobalDefaults::getDefaultCategoryImage
        );

        category.setCategoryImage(newImage);
        return category;
    }

    @Transactional
    public Category deleteCategoryImage(Long id) {
        Category category = this.findByIdHelper(id);

        if (!GlobalDefaults.isDefaultCategoryImage(category.getCategoryImage())){
            imageService.deleteFile(category.getCategoryImage().getS3Key());
            category.setCategoryImage(GlobalDefaults.getDefaultCategoryImage());
        }

        return category; // Saved automatically
    }

    @Transactional
    public Category save(Category category) {
        return categoryRepository.save(category);
    }

    // --- PRIVATE VALIDATION METHOD ---

    private void validateCircularReference(Category categoryToMove, Category newParentCandidate) {
        if (categoryToMove.getId().equals(newParentCandidate.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A category cannot be its own parent.");
        }

        Category temp = newParentCandidate;
        while (temp.getParent() != null) {
            if (temp.getParent().getId().equals(categoryToMove.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot move category into its own descendant.");
            }
            temp = temp.getParent();
        }
    }

    public Set<Long> findFamilyCategoryIds(Long rootId){
        return this.categoryRepository.findFamilyCategoryIds(rootId);
    }
}
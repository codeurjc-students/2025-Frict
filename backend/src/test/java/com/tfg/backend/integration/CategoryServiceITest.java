package com.tfg.backend.integration;

import com.tfg.backend.dto.CategoryDTO;
import com.tfg.backend.model.Category;
import com.tfg.backend.model.ImageInfo;
import com.tfg.backend.model.Product;
import com.tfg.backend.repository.CategoryRepository;
import com.tfg.backend.repository.ProductRepository;
import com.tfg.backend.service.CategoryService;
import com.tfg.backend.service.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests class for the CategoryService.
 * This class exhaustively covers the creation, update, and deletion of categories,
 * including complex database interactions like circular reference prevention,
 * cascade operations, and the automatic reassignment of orphaned products to the "Otros" category.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CategoryServiceITest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @MockitoBean
    private ImageService imageService;

    private Category otrosCategory;
    private Category rootCategory;
    private Category childCategory;
    private Product mockProduct;

    /**
     * Initial setup before each test.
     * Populates the MySQL database with the mandatory "Otros" category,
     * a hierarchical category tree, and a sample product to test reassignment logic.
     */
    @BeforeEach
    void setUp() {
        ImageInfo dummyImage = new ImageInfo("dummy.jpg", "dummy-url", "dummy.jpg");

        // 1. Create and save the mandatory "Otros" category
        otrosCategory = new Category("Otros", "icon", "banner", "short", "long");
        otrosCategory.setCategoryImage(dummyImage);
        categoryRepository.save(otrosCategory);

        // 2. Create a root category and a child category
        rootCategory = new Category("Electrónica", "icon", "banner", "short", "long");
        rootCategory.setCategoryImage(dummyImage);

        childCategory = new Category("Laptops", "icon", "banner", "short", "long");
        childCategory.setCategoryImage(dummyImage);

        rootCategory.addChild(childCategory);
        categoryRepository.save(rootCategory);

        // 3. Create a product and assign it to the child category
        mockProduct = new Product("Gaming Laptop", "High end", 1500.0, 1000.0);
        mockProduct.getCategories().add(childCategory);
        productRepository.save(mockProduct);
        childCategory.getProducts().add(mockProduct);
    }

    /**
     * Verifies that creating a new root category works and it persists correctly.
     */
    @Test
    @DisplayName("Create category without parent creates a root category in the database")
    void testCreateCategory_AsRoot_SavedInDatabase() {
        CategoryDTO dto = new CategoryDTO();
        dto.setName("Muebles");
        dto.setIcon("icon-furniture");

        Category savedCategory = categoryService.createCategory(dto);
        Category dbCategory = categoryRepository.findById(savedCategory.getId()).orElseThrow();

        assertAll(
                () -> assertNotNull(dbCategory.getId(), "The generated ID should not be null"),
                () -> assertEquals("Muebles", dbCategory.getName(), "Name mismatch"),
                () -> assertNull(dbCategory.getParent(), "Parent should be null for a root category")
        );
    }

    /**
     * Verifies that creating a new category with a specific parent properly establishes the relationship.
     */
    @Test
    @DisplayName("Create category with parent properly links the hierarchy in the database")
    void testCreateCategory_WithParent_SavedInDatabase() {
        CategoryDTO dto = new CategoryDTO();
        dto.setName("Smartphones");
        dto.setParentId(rootCategory.getId()); // Using "Electrónica" ID

        Category savedCategory = categoryService.createCategory(dto);
        Category dbCategory = categoryRepository.findById(savedCategory.getId()).orElseThrow();

        assertAll(
                () -> assertNotNull(dbCategory.getId()),
                () -> assertEquals("Smartphones", dbCategory.getName()),
                () -> assertNotNull(dbCategory.getParent(), "Parent should not be null"),
                () -> assertEquals(rootCategory.getId(), dbCategory.getParent().getId(), "Must be child of Electrónica")
        );
    }

    /**
     * Tests the strict protection against updating the "Otros" category.
     */
    @Test
    @DisplayName("Update category throws FORBIDDEN when attempting to modify 'Otros'")
    void testUpdateCategory_Otros_ThrowsForbidden() {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(otrosCategory.getId());
        dto.setName("Modified Otros");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> categoryService.updateCategory(otrosCategory.getId(), dto));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertNotNull(ex.getReason());
        assertTrue(ex.getReason().contains("Cannot update"));
    }

    /**
     * Validates that circular references are actively blocked by the database logic.
     */
    @Test
    @DisplayName("Update category throws FORBIDDEN to prevent circular hierarchy references")
    void testUpdateCategory_CircularReference_ThrowsForbidden() {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(rootCategory.getId());
        dto.setName("Electrónica");
        // Attempting to make the parent (Electrónica) a child of its own child (Laptops)
        dto.setParentId(childCategory.getId());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> categoryService.updateCategory(rootCategory.getId(), dto));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertNotNull(ex.getReason());
        assertTrue(ex.getReason().contains("descendant"));
    }

    /**
     * Validates complex business logic: If a category becomes a parent, its direct products are moved to "Otros".
     */
    @Test
    @DisplayName("Update category moves products to 'Otros' when the new parent previously had direct products")
    void testUpdateCategory_NewParentHadProducts_ProductsMovedToOtros() {
        // Setup: 'childCategory' (Laptops) currently has 'mockProduct'
        // We will create a new category and set 'Laptops' as its parent.
        Category newSubCategory = new Category("Gaming Accessories", "icon", "banner", "short", "long");
        categoryRepository.save(newSubCategory);

        CategoryDTO dto = new CategoryDTO();
        dto.setId(newSubCategory.getId());
        dto.setName("Gaming Accessories");
        dto.setParentId(childCategory.getId()); // Making Laptops the new parent

        // Act
        categoryService.updateCategory(newSubCategory.getId(), dto);

        // Assert
        Product updatedProduct = productRepository.findById(mockProduct.getId()).orElseThrow();
        Category others = categoryRepository.findByNameWithChildren("Otros").orElseThrow();

        assertAll(
                () -> assertFalse(updatedProduct.getCategories().stream().anyMatch(c -> c.getId().equals(childCategory.getId())),
                        "Product must be removed from Laptops because Laptops is now a parent"),
                () -> assertTrue(updatedProduct.getCategories().stream().anyMatch(c -> c.getId().equals(others.getId())),
                        "Product must have been automatically reassigned to the 'Otros' category")
        );
    }

    /**
     * Tests the strict protection against deleting the "Otros" category.
     */
    @Test
    @DisplayName("Delete category throws FORBIDDEN when attempting to delete 'Otros'")
    void testDeleteCategory_Otros_ThrowsForbidden() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> categoryService.deleteCategory(otrosCategory.getId()));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertNotNull(ex.getReason());
        assertTrue(ex.getReason().contains("Cannot delete"));
    }

    /**
     * Tests that deleting a category properly deletes children and moves orphaned products to "Otros".
     */
    @Test
    @DisplayName("Delete category successfully cascades deletions and moves orphaned products to 'Otros'")
    void testDeleteCategory_WithChildrenAndProducts_ProductsMovedToOtros() {
        // Act: Delete the root category. This should cascade and delete 'Laptops' too.
        categoryService.deleteCategory(rootCategory.getId());

        // Assert
        Optional<Category> deletedRoot = categoryRepository.findById(rootCategory.getId());
        Optional<Category> deletedChild = categoryRepository.findById(childCategory.getId());
        Product orphanedProduct = productRepository.findById(mockProduct.getId()).orElseThrow();
        Category others = categoryRepository.findByNameWithChildren("Otros").orElseThrow();

        assertAll(
                () -> assertTrue(deletedRoot.isEmpty(), "Root category must be completely deleted from the database"),
                () -> assertTrue(deletedChild.isEmpty(), "Child category must be deleted via cascade logic"),
                () -> assertTrue(orphanedProduct.getCategories().stream().anyMatch(c -> c.getId().equals(others.getId())),
                        "The orphaned product must have been moved to the 'Otros' category")
        );
    }
}
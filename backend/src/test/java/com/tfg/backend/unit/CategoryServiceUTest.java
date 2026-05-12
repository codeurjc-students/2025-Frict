package com.tfg.backend.unit;

import com.tfg.backend.dto.CategoryDTO;
import com.tfg.backend.model.Category;
import com.tfg.backend.model.ImageInfo;
import com.tfg.backend.model.Product;
import com.tfg.backend.repository.CategoryRepository;
import com.tfg.backend.service.CategoryService;
import com.tfg.backend.service.ImageService;
import com.tfg.backend.utils.GlobalDefaults;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceUTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private CategoryService categoryService;

    private Category othersCategory;
    private CategoryDTO categoryDTO;
    private MockedStatic<GlobalDefaults> mockedGlobalDefaults;

    @BeforeEach
    void setUp() {
        // Core 'Otros' category setup used across multiple business rules
        othersCategory = new Category();
        othersCategory.setId(99L);
        othersCategory.setName("Otros");

        // Default DTO for general use
        categoryDTO = new CategoryDTO();
        categoryDTO.setName("Test Category");
        categoryDTO.setId(1L);
    }

    // --- HELPER METHODS ---
    @Nested
    @DisplayName("Tests for findByIdHelper and findByNameHelper")
    class HelperTests {
        @Test
        @DisplayName("findByIdHelper throws 404 NOT_FOUND when category does not exist")
        void findByIdHelper_ThrowsNotFound() {
            when(categoryRepository.findByIdWithChildren(1L)).thenReturn(Optional.empty());
            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> categoryService.findByIdHelper(1L));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        @DisplayName("findByNameHelper throws 404 NOT_FOUND when category does not exist")
        void findByNameHelper_ThrowsNotFound() {
            when(categoryRepository.findByNameWithChildren("Test")).thenReturn(Optional.empty());
            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> categoryService.findByNameHelper("Test"));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    // --- CREATE CATEGORY ---
    @Nested
    @DisplayName("Tests for createCategory")
    class CreateCategoryTests {
        @Test
        @DisplayName("Throws FORBIDDEN if DTO ID matches 'Otros' category ID")
        void throwsForbiddenWhenIsOtros() {
            categoryDTO.setId(99L);
            when(categoryRepository.findByNameWithChildren("Otros")).thenReturn(Optional.of(othersCategory));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> categoryService.createCategory(categoryDTO));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Successfully creates root category when parentId is null")
        void successWithoutParent() {
            categoryDTO.setParentId(null);
            when(categoryRepository.findByNameWithChildren("Otros")).thenReturn(Optional.of(othersCategory));
            when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

            Category result = categoryService.createCategory(categoryDTO);

            assertNull(result.getParent());
            assertEquals("Test Category", result.getName());
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("Successfully creates nested category linked to its parent")
        void successWithParent() {
            categoryDTO.setParentId(10L);
            Category parent = new Category();
            parent.setId(10L);
            parent.setChildren(new ArrayList<>()); // Notice: ArrayList based on Category.java

            when(categoryRepository.findByNameWithChildren("Otros")).thenReturn(Optional.of(othersCategory));
            when(categoryRepository.findByIdWithChildren(10L)).thenReturn(Optional.of(parent));
            when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

            Category result = categoryService.createCategory(categoryDTO);

            verify(categoryRepository).save(any(Category.class));
            assertEquals(parent, result.getParent());
            assertTrue(parent.getChildren().contains(result));
        }
    }

    // --- UPDATE CATEGORY ---
    @Nested
    @DisplayName("Tests for updateCategory")
    class UpdateCategoryTests {
        private Category existingCategory;

        @BeforeEach
        void setupUpdate() {
            existingCategory = new Category();
            existingCategory.setId(1L);
            existingCategory.setName("Old Name");
        }

        @Test
        @DisplayName("Throws FORBIDDEN when attempting to update 'Otros'")
        void throwsForbiddenWhenUpdatingOtros() {
            categoryDTO.setId(99L);
            when(categoryRepository.findByIdWithChildren(99L)).thenReturn(Optional.of(othersCategory));
            when(categoryRepository.findByNameWithChildren("Otros")).thenReturn(Optional.of(othersCategory));

            assertThrows(ResponseStatusException.class, () -> categoryService.updateCategory(99L, categoryDTO));
        }

        @Test
        @DisplayName("Updates basic fields and removes parent if newParentId is null")
        void updatesBasicFieldsAndRemovesParent() {
            Category oldParent = mock(Category.class);
            existingCategory.setParent(oldParent);
            categoryDTO.setParentId(null);
            categoryDTO.setName("New Name");

            when(categoryRepository.findByIdWithChildren(1L)).thenReturn(Optional.of(existingCategory));
            when(categoryRepository.findByNameWithChildren("Otros")).thenReturn(Optional.of(othersCategory));

            Category result = categoryService.updateCategory(1L, categoryDTO);

            assertEquals("New Name", result.getName());
            verify(oldParent).removeChild(existingCategory);
        }

        @Test
        @DisplayName("Does not alter relations if newParentId is the same as the current one")
        void doesNotChangeRelationsWhenParentIsSame() {
            Category currentParent = new Category();
            currentParent.setId(10L);
            existingCategory.setParent(currentParent);
            categoryDTO.setParentId(10L);

            when(categoryRepository.findByIdWithChildren(1L)).thenReturn(Optional.of(existingCategory));
            when(categoryRepository.findByNameWithChildren("Otros")).thenReturn(Optional.of(othersCategory));

            categoryService.updateCategory(1L, categoryDTO);

            verify(categoryRepository, never()).findByIdWithChildren(10L);
        }

        @Test
        @DisplayName("Throws FORBIDDEN due to deep circular reference (Parent inside Grandchild)")
        void throwsForbiddenForDeepCircularReference() {
            Category newParentCandidate = new Category();
            newParentCandidate.setId(3L);

            Category middleChild = new Category();
            middleChild.setId(2L);
            middleChild.setParent(existingCategory); // Candidate descends from the category we are moving

            newParentCandidate.setParent(middleChild);

            categoryDTO.setParentId(3L);

            when(categoryRepository.findByIdWithChildren(1L)).thenReturn(Optional.of(existingCategory));
            when(categoryRepository.findByNameWithChildren("Otros")).thenReturn(Optional.of(othersCategory));
            when(categoryRepository.findByIdWithChildren(3L)).thenReturn(Optional.of(newParentCandidate));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> categoryService.updateCategory(1L, categoryDTO));
            assertEquals("Cannot move category into its own descendant.", ex.getReason());
        }

        @Test
        @DisplayName("Changes parent, clears products from new parent and moves them to 'Otros'")
        void changesParentAndMovesNewParentProductsToOtros() {
            // 1. Setup real objects instead of mocks for entities
            Category oldParent = new Category();
            oldParent.setId(5L);
            oldParent.setChildren(new ArrayList<>());
            oldParent.addChild(existingCategory); // This sets the parent symmetrically

            Category newParent = new Category();
            newParent.setId(10L);
            newParent.setChildren(new ArrayList<>());
            newParent.setProducts(new HashSet<>());

            Product product = new Product();
            product.setId(100L);
            // Products use HashSet
            product.setCategories(new ArrayList<>(List.of(newParent)));
            newParent.getProducts().add(product);

            categoryDTO.setParentId(10L);

            // 2. Mock only the repository behavior
            when(categoryRepository.findByIdWithChildren(1L)).thenReturn(Optional.of(existingCategory));
            when(categoryRepository.findByNameWithChildren("Otros")).thenReturn(Optional.of(othersCategory));
            when(categoryRepository.findByIdWithChildren(10L)).thenReturn(Optional.of(newParent));

            // 3. Act
            categoryService.updateCategory(1L, categoryDTO);

            // 4. Assert
            // Verify category was removed from old parent and added to new one
            assertFalse(oldParent.getChildren().contains(existingCategory), "Old parent should not contain the category");
            assertTrue(newParent.getChildren().contains(existingCategory), "New parent should contain the category");
            assertEquals(newParent, existingCategory.getParent(), "Category parent reference should be updated");

            // Verify product migration logic
            assertFalse(product.getCategories().contains(newParent), "Product should not be linked to new parent anymore");
            assertTrue(product.getCategories().contains(othersCategory), "Product should be moved to 'Otros' category");

            // This replaces verify(newParent.getProducts()).clear()
            assertTrue(newParent.getProducts().isEmpty(), "New parent's product list should be completely cleared");
        }
    }

    // --- DELETE CATEGORY & PROCESS DELETION ---
    @Nested
    @DisplayName("Tests for deleteCategory and processCategoryForDeletion")
    class DeleteCategoryTests {

        @BeforeEach
        void mockStaticGlobalDefaults() {
            mockedGlobalDefaults = mockStatic(GlobalDefaults.class);
        }

        @AfterEach
        void closeMockStatic() {
            mockedGlobalDefaults.close();
        }

        @Test
        @DisplayName("Throws FORBIDDEN when attempting to delete 'Otros'")
        void throwsForbiddenWhenDeletingOtros() {
            when(categoryRepository.findByIdWithChildren(99L)).thenReturn(Optional.of(othersCategory));
            when(categoryRepository.findByNameWithChildren("Otros")).thenReturn(Optional.of(othersCategory));

            assertThrows(ResponseStatusException.class, () -> categoryService.deleteCategory(99L));
        }

        @Test
        @DisplayName("Deletes category with custom image and removes S3 file")
        void deleteCategoryWithCustomImage() {
            Category categoryToDelete = new Category();
            categoryToDelete.setId(1L);
            Category parent = mock(Category.class);
            categoryToDelete.setParent(parent);

            ImageInfo customImage = new ImageInfo();
            customImage.setS3Key("custom-key.jpg");
            categoryToDelete.setCategoryImage(customImage);

            when(categoryRepository.findByIdWithChildren(1L)).thenReturn(Optional.of(categoryToDelete));
            when(categoryRepository.findByNameWithChildren("Otros")).thenReturn(Optional.of(othersCategory));
            mockedGlobalDefaults.when(() -> GlobalDefaults.isDefaultCategoryImage(customImage)).thenReturn(false);

            categoryService.deleteCategory(1L);

            verify(parent).removeChild(categoryToDelete);
            verify(imageService).deleteFile("custom-key.jpg");
            verify(categoryRepository).delete(categoryToDelete);
        }

        @Test
        @DisplayName("Deletes category with default image WITHOUT calling S3")
        void deleteCategoryWithDefaultImage() {
            Category categoryToDelete = new Category();
            categoryToDelete.setId(1L);
            ImageInfo defaultImage = new ImageInfo();
            categoryToDelete.setCategoryImage(defaultImage);

            when(categoryRepository.findByIdWithChildren(1L)).thenReturn(Optional.of(categoryToDelete));
            when(categoryRepository.findByNameWithChildren("Otros")).thenReturn(Optional.of(othersCategory));
            mockedGlobalDefaults.when(() -> GlobalDefaults.isDefaultCategoryImage(defaultImage)).thenReturn(true);

            categoryService.deleteCategory(1L);

            verify(imageService, never()).deleteFile(anyString());
            verify(categoryRepository).delete(categoryToDelete);
        }

        @Test
        @DisplayName("processCategoryForDeletion recursively processes children and migrates products")
        void processCategoryForDeletion_RecursiveAndMigratesProducts() {
            Category rootToDelete = new Category();
            Category childCategory = new Category();

            Product rootProduct = new Product();
            rootProduct.setCategories(new ArrayList<>(List.of(rootToDelete)));
            rootToDelete.setProducts(new HashSet<>(List.of(rootProduct)));

            Product childProduct = new Product();
            // A product that belongs to the child and another safe category
            Category safeCategory = new Category();
            childProduct.setCategories(new ArrayList<>(List.of(childCategory, safeCategory)));
            childCategory.setProducts(new HashSet<>(List.of(childProduct)));

            // Notice: Using ArrayList to match Category.java entity definition
            rootToDelete.setChildren(new ArrayList<>(List.of(childCategory)));

            categoryService.processCategoryForDeletion(rootToDelete, othersCategory);

            // Root validations
            assertTrue(rootToDelete.getProducts().isEmpty());
            assertTrue(rootProduct.getCategories().contains(othersCategory)); // Became empty, goes to 'Otros'

            // Child validations (Recursion check)
            assertTrue(childCategory.getProducts().isEmpty());
            assertFalse(childProduct.getCategories().contains(childCategory));
            assertTrue(childProduct.getCategories().contains(safeCategory));
            assertFalse(childProduct.getCategories().contains(othersCategory)); // Not moved to 'Otros' because it had safeCategory
        }
    }

    // --- IMAGE HANDLING ---
    @Nested
    @DisplayName("Tests for upload and delete CategoryImage")
    class ImageHandlingTests {
        private MockedStatic<GlobalDefaults> mockedGlobalDefaults;
        private Category category;

        @BeforeEach
        void setup() {
            mockedGlobalDefaults = mockStatic(GlobalDefaults.class);
            category = new Category();
            category.setId(1L);
            when(categoryRepository.findByIdWithChildren(1L)).thenReturn(Optional.of(category));
        }

        @AfterEach
        void teardown() {
            mockedGlobalDefaults.close();
        }

        @Test
        @DisplayName("uploadCategoryImage updates image through ImageService")
        void uploadCategoryImage_Success() {
            MultipartFile mockFile = mock(MultipartFile.class);
            ImageInfo newImage = new ImageInfo();

            // Mocking S3 logic delegation
            when(imageService.processImageReplacement(any(), eq(mockFile), eq("categories"), any(), any()))
                    .thenReturn(newImage);

            Category result = categoryService.uploadCategoryImage(1L, mockFile);

            assertEquals(newImage, result.getCategoryImage());
        }

        @Test
        @DisplayName("deleteCategoryImage deletes S3 file and restores default if it wasn't the default one")
        void deleteCategoryImage_WhenNotDefault() {
            ImageInfo customImage = new ImageInfo();
            customImage.setS3Key("old-image.png");
            category.setCategoryImage(customImage);

            ImageInfo defaultImage = new ImageInfo();

            mockedGlobalDefaults.when(() -> GlobalDefaults.isDefaultCategoryImage(customImage)).thenReturn(false);
            mockedGlobalDefaults.when(GlobalDefaults::getDefaultCategoryImage).thenReturn(defaultImage);

            Category result = categoryService.deleteCategoryImage(1L);

            verify(imageService).deleteFile("old-image.png");
            assertEquals(defaultImage, result.getCategoryImage());
        }
    }
}
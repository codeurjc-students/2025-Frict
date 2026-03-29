package com.tfg.backend.unit;

import com.tfg.backend.dto.CategoryDTO;
import com.tfg.backend.dto.ProductDTO;
import com.tfg.backend.model.*;
import com.tfg.backend.repository.ProductRepository;
import com.tfg.backend.service.*;
import com.tfg.backend.utils.GlobalDefaults;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceUTest {

    @Mock private ProductRepository productRepository;
    @Mock private UserService userService;
    @Mock private ShopStockService shopStockService;
    @Mock private CategoryService categoryService;
    @Mock private ImageService imageService;
    @Mock private OrderItemService orderItemService;

    @InjectMocks
    private ProductService productService;

    private User loggedUser;
    private Shop selectedShop;
    private Product product;
    private ProductDTO productDTO;
    private Category othersCategory;

    @BeforeEach
    void setUp() {
        selectedShop = new Shop();
        selectedShop.setId(10L);

        loggedUser = new User();
        loggedUser.setId(1L);
        loggedUser.setRoles(new HashSet<>(List.of("USER")));
        loggedUser.setSelectedShop(selectedShop);
        loggedUser.setFavouriteProducts(new HashSet<>());

        product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setCurrentPrice(10.0);
        product.setReferenceCode("REF-123");
        product.setActive(true);
        product.setCategories(new ArrayList<>());
        product.setImages(new ArrayList<>());
        product.setOrderItems(new ArrayList<>());

        productDTO = new ProductDTO();
        productDTO.setName("New Product");
        productDTO.setDescription("Description");
        productDTO.setCurrentPrice(15.0);
        productDTO.setSupplyPrice(5.0);
        productDTO.setActive(true);
        productDTO.setCategories(new ArrayList<>());

        othersCategory = new Category();
        othersCategory.setId(99L);
        othersCategory.setName("Otros");
        othersCategory.setChildren(new ArrayList<>()); // It is a leaf node
    }

    // --- ENRICH WITH STOCK TESTS ---
    @Nested
    @DisplayName("Tests for enrichWithStock logic")
    class EnrichWithStockTests {

        @Test
        @DisplayName("Returns 0 stock if user is not logged in or has no USER role")
        void enrichWithStock_ReturnsZero_WhenNotUserRole() {
            loggedUser.setRoles(new HashSet<>(List.of("ADMIN"))); // Not a USER
            when(userService.getLoggedUser()).thenReturn(Optional.of(loggedUser));

            Product enriched = productService.enrichWithStock(product);

            assertEquals(0, enriched.getAvailableUnits(), "Available units should be 0 for non-user roles");
            verifyNoInteractions(shopStockService);
        }

        @Test
        @DisplayName("Fetches real stock from ShopStockService when valid user and shop exist")
        void enrichWithStock_FetchesRealStock_WhenUserIsValid() {
            when(userService.getLoggedUser()).thenReturn(Optional.of(loggedUser));
            when(shopStockService.getLocalStock(product, 10L)).thenReturn(42);

            Product enriched = productService.enrichWithStock(product);

            assertEquals(42, enriched.getAvailableUnits(), "Available units should match shop stock");
        }

        @Test
        @DisplayName("Enriches a list of products correctly preserving the order")
        void enrichWithStockList_Success() {
            Product p2 = new Product(); p2.setId(2L);
            List<Product> products = List.of(product, p2);

            when(userService.getLoggedUser()).thenReturn(Optional.of(loggedUser));
            // Simulate that product 1 has 5 units, product 2 has 10
            when(shopStockService.getLocalStocks(products, 10L)).thenReturn(List.of(5, 10));

            List<Product> enrichedList = productService.enrichWithStock(products);

            assertEquals(5, enrichedList.get(0).getAvailableUnits());
            assertEquals(10, enrichedList.get(1).getAvailableUnits());
        }
    }

    // --- VALIDATION AND CRUD TESTS ---
    @Nested
    @DisplayName("Tests for internal validation and basic writing methods")
    class ValidationAndCrudTests {

        @Test
        @DisplayName("save throws IllegalArgumentException if reference code is duplicated")
        void save_ThrowsException_OnDuplicateReferenceCode() {
            when(productRepository.existsByReferenceCode("REF-123")).thenReturn(true);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> productService.save(product));
            assertEquals("The reference code is already taken", ex.getMessage());
        }

        @Test
        @DisplayName("save throws IllegalArgumentException if name is null or empty")
        void save_ThrowsException_OnInvalidName() {
            product.setName("");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> productService.save(product));
            assertEquals("The title is null or empty", ex.getMessage());
        }

        @Test
        @DisplayName("save throws IllegalArgumentException if price is negative")
        void save_ThrowsException_OnNegativePrice() {
            product.setCurrentPrice(-5.0);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> productService.save(product));
            assertEquals("The price should be positive or 0", ex.getMessage());
        }

        @Test
        @DisplayName("update throws EntityNotFoundException if product does not exist")
        void update_ThrowsException_WhenProductDoesNotExist() {
            when(productRepository.existsById(1L)).thenReturn(false);

            assertThrows(EntityNotFoundException.class, () -> productService.update(product));
        }

        @Test
        @DisplayName("deleteById throws EntityNotFoundException if product does not exist")
        void deleteById_ThrowsException() {
            when(productRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> productService.deleteById(1L));
        }
    }

    // --- FAVOURITES TESTS ---
    @Nested
    @DisplayName("Tests for User Favourites logic")
    class FavouritesTests {

        @BeforeEach
        void setupUser() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        }

        @Test
        @DisplayName("checkProductInFavourites returns true if product is in set")
        void checkProductInFavourites_ReturnsTrue() {
            loggedUser.getFavouriteProducts().add(product);
            assertTrue(productService.checkProductInFavourites(1L));
        }

        @Test
        @DisplayName("addProductToFavourites adds product to user's memory set")
        void addProductToFavourites_Success() {
            productService.addProductToFavourites(1L);
            assertTrue(loggedUser.getFavouriteProducts().contains(product), "Product should be added to favourites");
        }

        @Test
        @DisplayName("deleteProductFromFavourites removes product from user's memory set")
        void deleteProductFromFavourites_Success() {
            loggedUser.getFavouriteProducts().add(product);

            productService.deleteProductFromFavourites(1L);
            assertFalse(loggedUser.getFavouriteProducts().contains(product), "Product should be removed from favourites");
        }
    }

    // --- CATEGORY PROCESSING AND PRODUCT MUTATION ---
    @Nested
    @DisplayName("Tests for createProduct, updateProduct and private processCategories logic")
    class CategoryProcessingAndMutationTests {

        @Test
        @DisplayName("processCategories throws BAD_REQUEST if 'Otros' category is missing from DB")
        void processCategories_ThrowsBadRequest_WhenOtrosIsMissing() {
            when(categoryService.findByName("Otros")).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> productService.createProduct(productDTO));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertNotNull(ex.getReason());
            assertTrue(ex.getReason().contains("does not exist"));
        }

        @Test
        @DisplayName("processCategories throws BAD_REQUEST if a selected category ID is not found")
        void processCategories_ThrowsBadRequest_WhenSelectedCategoryIsMissing() {
            when(categoryService.findByName("Otros")).thenReturn(Optional.of(othersCategory));

            CategoryDTO fakeCategory = new CategoryDTO(); fakeCategory.setId(88L);
            productDTO.setCategories(List.of(fakeCategory));

            when(categoryService.findById(88L)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> productService.createProduct(productDTO));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        @DisplayName("createProduct assigns valid leaf categories correctly")
        void createProduct_AssignsValidLeafCategories() {
            when(categoryService.findByName("Otros")).thenReturn(Optional.of(othersCategory));

            CategoryDTO leafDto = new CategoryDTO(); leafDto.setId(5L);
            productDTO.setCategories(List.of(leafDto));

            Category leafCategory = new Category();
            leafCategory.setId(5L);
            leafCategory.setChildren(new ArrayList<>()); // It is a leaf node

            when(categoryService.findById(5L)).thenReturn(Optional.of(leafCategory));
            when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

            Product result = productService.createProduct(productDTO);

            assertEquals(1, result.getCategories().size());
            assertEquals(5L, result.getCategories().getFirst().getId(), "Should assign the valid leaf category");
        }

        @Test
        @DisplayName("updateProduct maps DTO fields to entity and processes categories")
        void updateProduct_MapsFieldsAndCategories() {
            when(categoryService.findByName("Otros")).thenReturn(Optional.of(othersCategory));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            // DTO carries no categories, should fallback to "Otros"
            productDTO.setCategories(null);
            productDTO.setName("Updated Name");
            productDTO.setActive(false);

            Product result = productService.updateProduct(1L, productDTO);

            // Assert field mapping
            assertEquals("Updated Name", result.getName());
            assertEquals("Description", result.getDescription());
            assertEquals(15.0, result.getCurrentPrice());
            assertEquals(5.0, result.getSupplyPrice());
            assertFalse(result.isActive());

            // Assert category processing fallback
            assertEquals(1, result.getCategories().size());
            assertEquals(99L, result.getCategories().getFirst().getId());
        }
    }

    // --- PRODUCT DELETION AND ACTIVATION ---
    @Nested
    @DisplayName("Tests for Deletion, Activation and Memory Unlinking")
    class DeletionAndActivationTests {

        @Test
        @DisplayName("deleteProduct clears categories, unlinks OrderItems and deletes S3 images")
        void deleteProduct_Success() {
            Category c = new Category();
            product.getCategories().add(c);

            OrderItem item = new OrderItem();
            item.setProduct(product);
            when(orderItemService.findByProductIdAndOrderIsNotNull(1L)).thenReturn(List.of(item));

            ProductImageInfo pii = mock(ProductImageInfo.class);
            when(pii.getS3Key()).thenReturn("custom-pic.jpg");
            product.getImages().add(pii);

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            try (MockedStatic<GlobalDefaults> mockedDefaults = mockStatic(GlobalDefaults.class)) {
                mockedDefaults.when(() -> GlobalDefaults.isDefaultProductImage(any())).thenReturn(false);

                productService.deleteProduct(1L);

                assertTrue(product.getCategories().isEmpty(), "Categories must be cleared");
                assertNull(item.getProduct(), "OrderItem must be unlinked from product");
                verify(imageService).deleteFile("custom-pic.jpg");
                verify(productRepository).delete(product);
            }
        }

        @Test
        @DisplayName("toggleGlobalActivation(false) deletes unpurchased cart items")
        void toggleGlobalActivation_False_RemovesCartItems() {
            OrderItem purchasedItem = new OrderItem();
            purchasedItem.setOrder(new Order());

            OrderItem cartItem = new OrderItem();
            cartItem.setOrder(null);

            product.getOrderItems().addAll(List.of(purchasedItem, cartItem));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            productService.toggleGlobalActivation(1L, false);

            assertFalse(product.isActive());
            assertEquals(1, product.getOrderItems().size(), "Only purchased items should remain");
            assertTrue(product.getOrderItems().contains(purchasedItem));
            verify(orderItemService).delete(cartItem); // Verifies the cart item was deleted from DB
        }

        @Test
        @DisplayName("toggleAllGlobalActivations applies state to all products and cleans carts if false")
        void toggleAllGlobalActivations_Success() {
            Product p2 = new Product();
            p2.setId(2L);
            p2.setOrderItems(new ArrayList<>());

            OrderItem cartItem = new OrderItem();
            cartItem.setOrder(null);
            product.getOrderItems().add(cartItem);

            // Mock getAllProducts which internally uses enrichWithStock and repository.findAll
            when(productRepository.findAll()).thenReturn(List.of(product, p2));
            when(userService.getLoggedUser()).thenReturn(Optional.empty()); // Bypasses stock enrichment for simplicity

            boolean finalState = productService.toggleAllGlobalActivations(false);

            assertFalse(finalState);
            assertFalse(product.isActive());
            assertFalse(p2.isActive());
            assertTrue(product.getOrderItems().isEmpty(), "Cart items must be cleared for all products");
            verify(orderItemService).delete(cartItem);
        }
    }

    // --- IMAGE HANDLING TESTS ---
    @Nested
    @DisplayName("Tests for Product Image updating and deletion")
    class ImageHandlingTests {

        @Test
        @DisplayName("deleteImage removes image, calls S3 and restores default if empty")
        void deleteImage_Success() {
            ProductImageInfo pii = new ProductImageInfo();
            pii.setId(5L);
            pii.getImageInfo().setS3Key("pic.jpg");
            product.getImages().add(pii);

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            try (MockedStatic<GlobalDefaults> mockedDefaults = mockStatic(GlobalDefaults.class)) {
                mockedDefaults.when(() -> GlobalDefaults.isDefaultProductImage(pii)).thenReturn(false);

                ImageInfo defaultImg = new ImageInfo();
                mockedDefaults.when(GlobalDefaults::getDefaultProductImage).thenReturn(defaultImg);

                productService.deleteImage(1L, 5L);

                verify(imageService).deleteFile("pic.jpg");
                assertEquals(1, product.getImages().size(), "Should have added the default image");
                assertEquals(defaultImg, product.getImages().getFirst().getImageInfo());
            }
        }

        @Test
        @DisplayName("deleteImage throws Exception if image ID is not found in product")
        void deleteImage_ThrowsException_IfImageNotFound() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(product)); // Product has empty images list

            RuntimeException ex = assertThrows(RuntimeException.class, () -> productService.deleteImage(1L, 99L));
            assertEquals("Image not found in this product.", ex.getMessage());
        }

        @Test
        @DisplayName("updateProductImages deletes missing S3 keys and uploads new files")
        void updateProductImages_Success() {
            // Existing image to keep
            ProductImageInfo keepImg = new ProductImageInfo();
            keepImg.getImageInfo().setS3Key("keep.jpg");

            // Existing image to discard
            ProductImageInfo discardImg = new ProductImageInfo();
            discardImg.getImageInfo().setS3Key("discard.jpg");

            product.getImages().addAll(new ArrayList<>(List.of(keepImg, discardImg)));

            // Request keeps 'keep.jpg'
            List<ProductImageInfo> requestExisting = List.of(keepImg);

            // Request adds a new file
            MultipartFile newFile = new MockMultipartFile("file", "new.jpg", "image/jpeg", new byte[0]);
            ImageInfo newS3Info = new ImageInfo();
            newS3Info.setS3Key("new.jpg");

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(imageService.uploadImageAndGetInfo(any(), eq("products"))).thenReturn(newS3Info);

            try (MockedStatic<GlobalDefaults> mockedDefaults = mockStatic(GlobalDefaults.class)) {
                mockedDefaults.when(() -> GlobalDefaults.isDefaultProductImage(any())).thenReturn(false);

                productService.updateProductImages(1L, requestExisting, List.of(newFile));

                // Assertions
                verify(imageService).deleteFile("discard.jpg"); // Discarded was deleted
                assertEquals(2, product.getImages().size()); // 'keep.jpg' + 'new.jpg'

                // Extract keys to check
                List<String> finalKeys = product.getImages().stream().map(ProductImageInfo::getS3Key).toList();
                assertTrue(finalKeys.contains("keep.jpg"));
                assertTrue(finalKeys.contains("new.jpg"));
            }
        }
    }
}
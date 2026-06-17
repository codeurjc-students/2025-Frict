package com.tfg.backend.unit;

import com.tfg.backend.dto.CategoryDTO;
import com.tfg.backend.dto.ProductDTO;
import com.tfg.backend.dto.ProductSpecDTO;
import com.tfg.backend.event.RegistryEvent;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceUTest {

    @Mock private ProductRepository productRepository;
    @Mock private RegistryService registryService;
    @Mock private UserService userService;
    @Mock private ShopStockService shopStockService;
    @Mock private CategoryService categoryService;
    @Mock private ImageService imageService;
    @Mock private OrderItemService orderItemService;
    @Mock private ApplicationEventPublisher eventPublisher;

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
        selectedShop.setReferenceCode("SH-REF");
        selectedShop.setName("Test Shop");

        loggedUser = new User();
        loggedUser.setId(1L);
        loggedUser.setUsername("test-user");
        loggedUser.setName("Test User");
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
        product.setShopsStock(new ArrayList<>());

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
        othersCategory.setChildren(new ArrayList<>());
    }

    // --- READ OPERATIONS ---
    @Nested
    @DisplayName("Read-only delegation and helper tests")
    class ReadOperationsTests {

        @Test
        @DisplayName("getAllProducts (no-arg) delegates to repository and enriches stock")
        void getAllProducts_NoArg_DelegatesAndEnriches() {
            when(productRepository.findAll()).thenReturn(List.of(product));
            when(userService.getLoggedUser()).thenReturn(Optional.empty());

            List<Product> result = productService.getAllProducts();

            assertEquals(1, result.size());
            assertEquals(0, result.getFirst().getAvailableUnits());
            verify(productRepository).findAll();
        }

        @Test
        @DisplayName("getAllProducts (pageable) delegates to repository and enriches stock")
        void getAllProducts_WithPageable_DelegatesAndEnriches() {
            Pageable pageable = PageRequest.of(0, 5);
            Page<Product> page = new PageImpl<>(List.of(product));
            when(productRepository.findAll(pageable)).thenReturn(page);
            when(userService.getLoggedUser()).thenReturn(Optional.empty());

            Page<Product> result = productService.getAllProducts(pageable);

            assertEquals(1, result.getTotalElements());
            verify(productRepository).findAll(pageable);
        }

        @Test
        @DisplayName("getFilteredProducts builds a Specification and delegates to JpaSpecificationExecutor")
        void getFilteredProducts_ForwardsFilters() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> page = new PageImpl<>(List.of(product));
            when(productRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
            when(userService.getLoggedUser()).thenReturn(Optional.empty());

            Page<Product> result = productService.getFilteredProducts("laptop", List.of(1L, 2L), List.of(), pageable);

            assertEquals(1, result.getTotalElements());
            verify(productRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("getSpecsCatalog delegates to repository and assembles ordered name→values map")
        void getSpecsCatalog_DelegatesToRepository() {
            when(productRepository.findAllDistinctSpecNames()).thenReturn(List.of("Color", "Talla"));
            when(productRepository.findDistinctValuesBySpecName("Color")).thenReturn(List.of("Azul", "Rojo"));
            when(productRepository.findDistinctValuesBySpecName("Talla")).thenReturn(List.of("M", "XL"));

            Map<String, List<String>> catalog = productService.getSpecsCatalog();

            assertEquals(2, catalog.size());
            assertEquals(List.of("Azul", "Rojo"), catalog.get("Color"));
            assertEquals(List.of("M", "XL"), catalog.get("Talla"));
            verify(productRepository).findAllDistinctSpecNames();
            verify(productRepository).findDistinctValuesBySpecName("Color");
            verify(productRepository).findDistinctValuesBySpecName("Talla");
        }

        @Test
        @DisplayName("findById returns enriched Optional when product exists")
        void findById_ReturnsEnrichedProduct_WhenFound() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(userService.getLoggedUser()).thenReturn(Optional.empty());

            Optional<Product> result = productService.findById(1L);

            assertTrue(result.isPresent());
            assertEquals(0, result.get().getAvailableUnits());
        }

        @Test
        @DisplayName("findById returns empty Optional when product does not exist")
        void findById_ReturnsEmpty_WhenNotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertTrue(productService.findById(99L).isEmpty());
        }

        @Test
        @DisplayName("findProductHelper throws NOT_FOUND when product does not exist")
        void findProductHelper_ThrowsNotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> productService.findProductHelper(99L));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        @DisplayName("findProductsNotAssignedToShop delegates to repository")
        void findProductsNotAssignedToShop_DelegatesToRepository() {
            when(productRepository.findProductsNotAssignedToShop(10L)).thenReturn(List.of(product));

            List<Product> result = productService.findProductsNotAssignedToShop(10L);

            assertEquals(1, result.size());
            verify(productRepository).findProductsNotAssignedToShop(10L);
        }

        @Test
        @DisplayName("getUserFavouriteProducts delegates to repository with logged user's ID")
        void getUserFavouriteProducts_DelegatesToRepository() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> page = new PageImpl<>(List.of(product));
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(productRepository.findFavouritesByUserId(1L, pageable)).thenReturn(page);

            Page<Product> result = productService.getUserFavouriteProducts(pageable);

            assertEquals(1, result.getTotalElements());
        }
    }

    // --- ENRICH WITH STOCK TESTS ---
    @Nested
    @DisplayName("Tests for enrichWithStock logic")
    class EnrichWithStockTests {

        @Test
        @DisplayName("Returns 0 stock if user has no USER role")
        void enrichWithStock_ReturnsZero_WhenNotUserRole() {
            loggedUser.setRoles(new HashSet<>(List.of("ADMIN")));
            when(userService.getLoggedUser()).thenReturn(Optional.of(loggedUser));

            Product enriched = productService.enrichWithStock(product);

            assertEquals(0, enriched.getAvailableUnits());
            verifyNoInteractions(shopStockService);
        }

        @Test
        @DisplayName("Returns 0 stock when no user is logged in")
        void enrichWithStock_ReturnsZero_WhenNotLoggedIn() {
            when(userService.getLoggedUser()).thenReturn(Optional.empty());

            Product enriched = productService.enrichWithStock(product);

            assertEquals(0, enriched.getAvailableUnits());
        }

        @Test
        @DisplayName("Returns 0 when ShopStockService returns null for the product")
        void enrichWithStock_ReturnsZero_WhenStockIsNull() {
            when(userService.getLoggedUser()).thenReturn(Optional.of(loggedUser));
            when(shopStockService.getLocalStock(product, 10L)).thenReturn(null);

            Product enriched = productService.enrichWithStock(product);

            assertEquals(0, enriched.getAvailableUnits());
        }

        @Test
        @DisplayName("Passes null shopId to ShopStockService when logged user has no selected shop")
        void enrichWithStock_UsesNullShopId_WhenNoShopSelected() {
            loggedUser.setSelectedShop(null);
            when(userService.getLoggedUser()).thenReturn(Optional.of(loggedUser));
            when(shopStockService.getLocalStock(product, null)).thenReturn(3);

            Product enriched = productService.enrichWithStock(product);

            assertEquals(3, enriched.getAvailableUnits());
            verify(shopStockService).getLocalStock(product, null);
        }

        @Test
        @DisplayName("Fetches real stock from ShopStockService when valid user and shop exist")
        void enrichWithStock_FetchesRealStock_WhenUserIsValid() {
            when(userService.getLoggedUser()).thenReturn(Optional.of(loggedUser));
            when(shopStockService.getLocalStock(product, 10L)).thenReturn(42);

            Product enriched = productService.enrichWithStock(product);

            assertEquals(42, enriched.getAvailableUnits());
        }

        @Test
        @DisplayName("Enriches a list of products correctly preserving index order")
        void enrichWithStockList_Success() {
            Product p2 = new Product(); p2.setId(2L);
            List<Product> products = List.of(product, p2);

            when(userService.getLoggedUser()).thenReturn(Optional.of(loggedUser));
            when(shopStockService.getLocalStocks(products, 10L)).thenReturn(List.of(5, 10));

            List<Product> enrichedList = productService.enrichWithStock(products);

            assertEquals(5, enrichedList.get(0).getAvailableUnits());
            assertEquals(10, enrichedList.get(1).getAvailableUnits());
        }

        @Test
        @DisplayName("Empty list is returned immediately without touching user or stock services")
        void enrichWithStockList_EmptyList_ReturnsImmediately() {
            List<Product> result = productService.enrichWithStock(new ArrayList<>());

            assertTrue(result.isEmpty());
            verifyNoInteractions(userService, shopStockService);
        }

        @Test
        @DisplayName("Sets 0 for all list items when no user is logged in")
        void enrichWithStockList_SetsZero_WhenNotLoggedIn() {
            when(userService.getLoggedUser()).thenReturn(Optional.empty());

            List<Product> result = productService.enrichWithStock(List.of(product));

            assertEquals(0, result.getFirst().getAvailableUnits());
            verifyNoInteractions(shopStockService);
        }

        @Test
        @DisplayName("Sets 0 for a list slot when ShopStockService returns null for that index")
        void enrichWithStockList_SetsZero_WhenStockIsNull() {
            when(userService.getLoggedUser()).thenReturn(Optional.of(loggedUser));
            when(shopStockService.getLocalStocks(List.of(product), 10L))
                    .thenReturn(Collections.singletonList(null));

            List<Product> result = productService.enrichWithStock(List.of(product));

            assertEquals(0, result.getFirst().getAvailableUnits());
        }
    }

    // --- GET PRODUCT BY ID (view registry tracking) ---
    @Nested
    @DisplayName("Tests for getProductById view registry event logic")
    class GetProductByIdTests {

        @BeforeEach
        void stubFind() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(userService.getLoggedUser()).thenReturn(Optional.empty());
        }

        @Test
        @DisplayName("Fires RegistryEvent including shop data when logged user has a selected shop")
        void getProductById_FiresEvent_LoggedUserWithShop() {
            when(userService.getLoggedUserUsername()).thenReturn("test-user");
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

            Product result = productService.getProductById(1L);

            assertSame(product, result);
            verify(eventPublisher).publishEvent(any(RegistryEvent.class));
        }

        @Test
        @DisplayName("Fires RegistryEvent without shop data when logged user has no selected shop")
        void getProductById_FiresEvent_LoggedUserWithoutShop() {
            loggedUser.setSelectedShop(null);
            when(userService.getLoggedUserUsername()).thenReturn("test-user");
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

            Product result = productService.getProductById(1L);

            assertSame(product, result);
            verify(eventPublisher).publishEvent(any(RegistryEvent.class));
        }

        @Test
        @DisplayName("Fires anonymous RegistryEvent when no user is logged in")
        void getProductById_FiresAnonymousEvent_WhenNotLoggedIn() {
            when(userService.getLoggedUserUsername()).thenReturn(null);

            Product result = productService.getProductById(1L);

            assertSame(product, result);
            verify(eventPublisher).publishEvent(any(RegistryEvent.class));
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

            assertThrows(IllegalArgumentException.class, () -> productService.save(product));
        }

        @Test
        @DisplayName("save throws IllegalArgumentException if name is null or empty")
        void save_ThrowsException_OnInvalidName() {
            product.setName("");

            assertThrows(IllegalArgumentException.class, () -> productService.save(product));
        }

        @Test
        @DisplayName("save throws IllegalArgumentException if price is negative")
        void save_ThrowsException_OnNegativePrice() {
            product.setCurrentPrice(-5.0);

            assertThrows(IllegalArgumentException.class, () -> productService.save(product));
        }

        @Test
        @DisplayName("save persists and returns product when all fields are valid")
        void save_Success_PersistsProduct() {
            when(productRepository.existsByReferenceCode("REF-123")).thenReturn(false);
            when(productRepository.save(product)).thenReturn(product);

            Product result = productService.save(product);

            assertSame(product, result);
            verify(productRepository).save(product);
        }

        @Test
        @DisplayName("update throws EntityNotFoundException if product does not exist")
        void update_ThrowsException_WhenProductDoesNotExist() {
            when(productRepository.existsById(1L)).thenReturn(false);

            assertThrows(EntityNotFoundException.class, () -> productService.update(product));
        }

        @Test
        @DisplayName("update persists product when it exists and fields are valid")
        void update_Success_UpdatesProduct() {
            when(productRepository.existsById(1L)).thenReturn(true);
            when(productRepository.save(product)).thenReturn(product);

            Product result = productService.update(product);

            assertSame(product, result);
            verify(productRepository).save(product);
        }

        @Test
        @DisplayName("deleteById throws EntityNotFoundException if product does not exist")
        void deleteById_ThrowsException() {
            when(productRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResponseStatusException.class, () -> productService.deleteProduct(1L));
        }

        @Test
        @DisplayName("deleteById deletes the product when found")
        void deleteById_Success() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            productService.deleteProduct(1L);

            verify(productRepository).delete(product);
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
            when(userService.getLoggedUser()).thenReturn(Optional.empty());
        }

        @Test
        @DisplayName("checkProductInFavourites returns true if product is in set")
        void checkProductInFavourites_ReturnsTrue() {
            loggedUser.getFavouriteProducts().add(product);
            assertTrue(productService.checkProductInFavourites(1L));
        }

        @Test
        @DisplayName("checkProductInFavourites returns false if product is NOT in set")
        void checkProductInFavourites_ReturnsFalse() {
            assertFalse(productService.checkProductInFavourites(1L));
        }

        @Test
        @DisplayName("addProductToFavourites adds product to user's memory set")
        void addProductToFavourites_Success() {
            productService.addProductToFavourites(1L);
            assertTrue(loggedUser.getFavouriteProducts().contains(product));
        }

        @Test
        @DisplayName("deleteProductFromFavourites removes product from user's memory set")
        void deleteProductFromFavourites_Success() {
            loggedUser.getFavouriteProducts().add(product);

            productService.deleteProductFromFavourites(1L);
            assertFalse(loggedUser.getFavouriteProducts().contains(product));
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

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> productService.createProduct(productDTO));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        @DisplayName("processCategories throws BAD_REQUEST if a selected category ID is not found")
        void processCategories_ThrowsBadRequest_WhenSelectedCategoryIsMissing() {
            when(categoryService.findByName("Otros")).thenReturn(Optional.of(othersCategory));

            CategoryDTO fakeCategory = new CategoryDTO(); fakeCategory.setId(88L);
            productDTO.setCategories(List.of(fakeCategory));

            when(categoryService.findById(88L)).thenReturn(Optional.empty());

            assertThrows(ResponseStatusException.class, () -> productService.createProduct(productDTO));
        }

        @Test
        @DisplayName("createProduct assigns valid leaf categories correctly")
        void createProduct_AssignsValidLeafCategories() {
            when(categoryService.findByName("Otros")).thenReturn(Optional.of(othersCategory));

            CategoryDTO leafDto = new CategoryDTO(); leafDto.setId(5L);
            productDTO.setCategories(List.of(leafDto));

            Category leafCategory = new Category();
            leafCategory.setId(5L);
            leafCategory.setChildren(new ArrayList<>());

            when(categoryService.findById(5L)).thenReturn(Optional.of(leafCategory));
            when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

            Product result = productService.createProduct(productDTO);

            assertEquals(1, result.getCategories().size());
            assertEquals(5L, result.getCategories().getFirst().getId());
        }

        @Test
        @DisplayName("processCategories falls back to 'Otros' when all provided categories are parent nodes (have children)")
        void processCategories_FallsBackToOthers_WhenAllCategoriesAreParentNodes() {
            when(categoryService.findByName("Otros")).thenReturn(Optional.of(othersCategory));

            CategoryDTO parentDto = new CategoryDTO(); parentDto.setId(10L);
            productDTO.setCategories(List.of(parentDto));

            Category parentCategory = new Category();
            parentCategory.setId(10L);
            Category child = new Category(); child.setId(11L);
            parentCategory.setChildren(List.of(child));

            when(categoryService.findById(10L)).thenReturn(Optional.of(parentCategory));
            when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

            Product result = productService.createProduct(productDTO);

            assertEquals(1, result.getCategories().size());
            assertEquals(99L, result.getCategories().getFirst().getId(),
                    "Should fall back to 'Otros' when all submitted categories are parent nodes");
        }

        @Test
        @DisplayName("createProduct falls back to 'Otros' when categories list is null")
        void createProduct_FallsBackToOthers_WhenNoCategoriesProvided() {
            when(categoryService.findByName("Otros")).thenReturn(Optional.of(othersCategory));
            productDTO.setCategories(null);
            when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

            Product result = productService.createProduct(productDTO);

            assertEquals(1, result.getCategories().size());
            assertEquals(99L, result.getCategories().getFirst().getId());
        }

        @Test
        @DisplayName("createProduct maps ProductSpecDTO list to ProductSpec entities on the saved product")
        void createProduct_WithSpecs_PersistsSpecifications() {
            when(categoryService.findByName("Otros")).thenReturn(Optional.of(othersCategory));
            productDTO.setSpecifications(List.of(new ProductSpecDTO(null, "Color", List.of("Rojo", "Azul"))));
            when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

            Product result = productService.createProduct(productDTO);

            assertEquals(1, result.getSpecifications().size());
            ProductSpec spec = result.getSpecifications().getFirst();
            assertEquals("Color", spec.getName());
            assertTrue(spec.getValues().containsAll(List.of("Rojo", "Azul")));
        }

        @Test
        @DisplayName("updateProduct replaces existing specifications with the ones from the incoming DTO")
        void updateProduct_WithSpecs_ReplacesSpecifications() {
            ProductSpec existingSpec = new ProductSpec("Old", List.of("V1"), product);
            product.getSpecifications().add(existingSpec);

            when(categoryService.findByName("Otros")).thenReturn(Optional.of(othersCategory));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(userService.getLoggedUser()).thenReturn(Optional.empty());

            productDTO.setCategories(null);
            productDTO.setSpecifications(List.of(new ProductSpecDTO(null, "Talla", List.of("M", "XL"))));

            Product result = productService.updateProduct(1L, productDTO);

            assertEquals(1, result.getSpecifications().size());
            ProductSpec updated = result.getSpecifications().getFirst();
            assertEquals("Talla", updated.getName());
            assertTrue(updated.getValues().containsAll(List.of("M", "XL")));
        }

        @Test
        @DisplayName("updateProduct updates an existing specification in place when the DTO id matches an existing spec")
        void updateProduct_WithSpecs_UpdatesExistingSpecById() {
            ProductSpec existingSpec = new ProductSpec("Color", List.of("Rojo"), product);
            existingSpec.setId(42L);
            product.getSpecifications().add(existingSpec);

            when(categoryService.findByName("Otros")).thenReturn(Optional.of(othersCategory));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(userService.getLoggedUser()).thenReturn(Optional.empty());

            productDTO.setCategories(null);
            productDTO.setSpecifications(List.of(new ProductSpecDTO(42L, "Color", List.of("Verde", "Negro"))));

            Product result = productService.updateProduct(1L, productDTO);

            assertEquals(1, result.getSpecifications().size(), "Spec must be updated in place, not duplicated");
            ProductSpec updated = result.getSpecifications().getFirst();
            assertSame(existingSpec, updated, "The same managed spec entity must be reused");
            assertEquals(42L, updated.getId());
            assertEquals(List.of("Verde", "Negro"), updated.getValues());
        }

        @Test
        @DisplayName("createProduct skips specification DTOs with a blank name or empty values")
        void createProduct_SkipsInvalidSpecDtos() {
            when(categoryService.findByName("Otros")).thenReturn(Optional.of(othersCategory));
            when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

            productDTO.setSpecifications(List.of(
                    new ProductSpecDTO(null, "   ", List.of("X")),        // blank name → skipped
                    new ProductSpecDTO(null, "Talla", List.of()),         // empty values → skipped
                    new ProductSpecDTO(null, "Color", List.of("Rojo"))    // valid → kept
            ));

            Product result = productService.createProduct(productDTO);

            assertEquals(1, result.getSpecifications().size(), "Only the single valid spec must be added");
            assertEquals("Color", result.getSpecifications().getFirst().getName());
        }

        @Test
        @DisplayName("updateProduct with null specifications removes all pre-existing specs")
        void updateProduct_NullSpecs_RemovesExistingSpecs() {
            ProductSpec existingSpec = new ProductSpec("Old", List.of("V1"), product);
            existingSpec.setId(7L);
            product.getSpecifications().add(existingSpec);

            when(categoryService.findByName("Otros")).thenReturn(Optional.of(othersCategory));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(userService.getLoggedUser()).thenReturn(Optional.empty());

            productDTO.setCategories(null);
            productDTO.setSpecifications(null);

            Product result = productService.updateProduct(1L, productDTO);

            assertTrue(result.getSpecifications().isEmpty(), "All specs must be removed when DTO specifications is null");
        }

        @Test
        @DisplayName("updateProduct maps DTO fields to entity and falls back to 'Otros' for null categories")
        void updateProduct_MapsFieldsAndCategories() {
            when(categoryService.findByName("Otros")).thenReturn(Optional.of(othersCategory));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(userService.getLoggedUser()).thenReturn(Optional.empty());

            productDTO.setCategories(null);
            productDTO.setName("Updated Name");
            productDTO.setActive(false);

            Product result = productService.updateProduct(1L, productDTO);

            assertAll(
                    () -> assertEquals("Updated Name", result.getName()),
                    () -> assertEquals("Description", result.getDescription()),
                    () -> assertEquals(15.0, result.getCurrentPrice()),
                    () -> assertEquals(5.0, result.getSupplyPrice()),
                    () -> assertFalse(result.isActive()),
                    () -> assertEquals(1, result.getCategories().size()),
                    () -> assertEquals(99L, result.getCategories().getFirst().getId())
            );
        }
    }

    // --- PRODUCT DELETION AND ACTIVATION ---
    @Nested
    @DisplayName("Tests for Deletion, Activation and Memory Unlinking")
    class DeletionAndActivationTests {

        @Test
        @DisplayName("deleteProduct clears categories, unlinks historical OrderItems, deletes cart items and S3 images")
        void deleteProduct_Success() {
            product.getCategories().add(new Category());

            OrderItem historicalItem = new OrderItem();
            historicalItem.setProduct(product);
            historicalItem.setOrder(new Order());
            product.getOrderItems().add(historicalItem);

            OrderItem cartItem = new OrderItem();
            cartItem.setProduct(product);
            cartItem.setOrder(null);
            product.getOrderItems().add(cartItem);

            ProductImageInfo pii = mock(ProductImageInfo.class);
            when(pii.getS3Key()).thenReturn("custom-pic.jpg");
            product.getImages().add(pii);

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(userService.getLoggedUser()).thenReturn(Optional.empty());

            try (MockedStatic<GlobalDefaults> mockedDefaults = mockStatic(GlobalDefaults.class)) {
                mockedDefaults.when(() -> GlobalDefaults.isDefaultProductImage(any())).thenReturn(false);

                productService.deleteProduct(1L);

                assertTrue(product.getCategories().isEmpty());
                assertNull(historicalItem.getProduct());
                verify(orderItemService).delete(cartItem);
                verify(imageService).deleteFile("custom-pic.jpg");
                verify(productRepository).delete(product);
            }
        }

        @Test
        @DisplayName("deleteProduct does NOT delete S3 file when image is the default product image")
        void deleteProduct_DoesNotDeleteDefaultImage() {
            product.getImages().add(new ProductImageInfo());

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(userService.getLoggedUser()).thenReturn(Optional.empty());

            try (MockedStatic<GlobalDefaults> mockedDefaults = mockStatic(GlobalDefaults.class)) {
                mockedDefaults.when(() -> GlobalDefaults.isDefaultProductImage(any())).thenReturn(true);

                productService.deleteProduct(1L);

                verify(imageService, never()).deleteFile(any());
                verify(productRepository).delete(product);
            }
        }

        @Test
        @DisplayName("toggleGlobalActivation(false) deactivates product and deletes unpurchased cart items")
        void toggleGlobalActivation_False_RemovesCartItems() {
            OrderItem purchasedItem = new OrderItem();
            purchasedItem.setOrder(new Order());

            OrderItem cartItem = new OrderItem();
            cartItem.setOrder(null);

            product.getOrderItems().addAll(List.of(purchasedItem, cartItem));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(userService.getLoggedUser()).thenReturn(Optional.empty());

            productService.toggleGlobalActivation(1L, false);

            assertFalse(product.isActive());
            assertEquals(1, product.getOrderItems().size());
            assertTrue(product.getOrderItems().contains(purchasedItem));
            verify(orderItemService).delete(cartItem);
        }

        @Test
        @DisplayName("toggleGlobalActivation(true) sets product active without touching any order items")
        void toggleGlobalActivation_True_DoesNotDeleteItems() {
            OrderItem cartItem = new OrderItem();
            cartItem.setOrder(null);
            product.getOrderItems().add(cartItem);
            product.setActive(false);

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(userService.getLoggedUser()).thenReturn(Optional.empty());

            productService.toggleGlobalActivation(1L, true);

            assertTrue(product.isActive());
            assertEquals(1, product.getOrderItems().size(), "Cart items must NOT be deleted when activating");
            verify(orderItemService, never()).delete(any());
        }

        @Test
        @DisplayName("toggleAllGlobalActivations(false) deactivates all products and cleans cart items")
        void toggleAllGlobalActivations_False_CleansCartItems() {
            Product p2 = new Product();
            p2.setId(2L);
            p2.setOrderItems(new ArrayList<>());

            OrderItem cartItem = new OrderItem();
            cartItem.setOrder(null);
            product.getOrderItems().add(cartItem);

            when(productRepository.findAll()).thenReturn(List.of(product, p2));
            when(userService.getLoggedUser()).thenReturn(Optional.empty());

            boolean finalState = productService.toggleAllGlobalActivations(false);

            assertAll(
                    () -> assertFalse(finalState),
                    () -> assertFalse(product.isActive()),
                    () -> assertFalse(p2.isActive()),
                    () -> assertTrue(product.getOrderItems().isEmpty())
            );
            verify(orderItemService).delete(cartItem);
        }

        @Test
        @DisplayName("toggleAllGlobalActivations(true) activates all products without deleting cart items")
        void toggleAllGlobalActivations_True_SetsAllActiveWithoutDeletion() {
            product.setActive(false);
            OrderItem cartItem = new OrderItem();
            cartItem.setOrder(null);
            product.getOrderItems().add(cartItem);

            when(productRepository.findAll()).thenReturn(List.of(product));
            when(userService.getLoggedUser()).thenReturn(Optional.empty());

            boolean result = productService.toggleAllGlobalActivations(true);

            assertTrue(result);
            assertTrue(product.isActive());
            assertEquals(1, product.getOrderItems().size(), "Cart items must NOT be deleted when activating");
            verify(orderItemService, never()).delete(any());
        }
    }

    // --- IMAGE HANDLING TESTS ---
    @Nested
    @DisplayName("Tests for Product Image updating and deletion")
    class ImageHandlingTests {

        @Test
        @DisplayName("deleteImage removes image, calls S3 and restores default when list becomes empty")
        void deleteImage_Success() {
            ProductImageInfo pii = new ProductImageInfo();
            pii.setId(5L);
            pii.getImageInfo().setS3Key("pic.jpg");
            product.getImages().add(pii);

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(userService.getLoggedUser()).thenReturn(Optional.empty());

            try (MockedStatic<GlobalDefaults> mockedDefaults = mockStatic(GlobalDefaults.class)) {
                mockedDefaults.when(() -> GlobalDefaults.isDefaultProductImage(pii)).thenReturn(false);
                ImageInfo defaultImg = new ImageInfo();
                mockedDefaults.when(GlobalDefaults::getDefaultProductImage).thenReturn(defaultImg);

                productService.deleteImage(1L, 5L);

                verify(imageService).deleteFile("pic.jpg");
                assertEquals(1, product.getImages().size(), "Default image must be added after last image deletion");
                assertEquals(defaultImg, product.getImages().getFirst().getImageInfo());
            }
        }

        @Test
        @DisplayName("deleteImage does NOT call S3 or remove the image when it is the default product image")
        void deleteImage_DoesNotDelete_WhenDefaultImage() {
            ProductImageInfo defaultImg = new ProductImageInfo();
            defaultImg.setId(5L);
            product.getImages().add(defaultImg);

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(userService.getLoggedUser()).thenReturn(Optional.empty());

            try (MockedStatic<GlobalDefaults> mockedDefaults = mockStatic(GlobalDefaults.class)) {
                mockedDefaults.when(() -> GlobalDefaults.isDefaultProductImage(defaultImg)).thenReturn(true);

                productService.deleteImage(1L, 5L);

                verify(imageService, never()).deleteFile(any());
                assertEquals(1, product.getImages().size(), "Default image must remain in the list");
            }
        }

        @Test
        @DisplayName("deleteImage throws RuntimeException if image ID is not found in product")
        void deleteImage_ThrowsException_IfImageNotFound() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(userService.getLoggedUser()).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> productService.deleteImage(1L, 99L));
            assertEquals("Image not found in this product.", ex.getMessage());
        }

        @Test
        @DisplayName("updateProductImages deletes missing S3 keys and uploads new files")
        void updateProductImages_Success() {
            ProductImageInfo keepImg = new ProductImageInfo();
            keepImg.getImageInfo().setS3Key("keep.jpg");

            ProductImageInfo discardImg = new ProductImageInfo();
            discardImg.getImageInfo().setS3Key("discard.jpg");

            product.getImages().addAll(new ArrayList<>(List.of(keepImg, discardImg)));

            MultipartFile newFile = new MockMultipartFile("file", "new.jpg", "image/jpeg", new byte[0]);
            ImageInfo newS3Info = new ImageInfo();
            newS3Info.setS3Key("new.jpg");

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(userService.getLoggedUser()).thenReturn(Optional.empty());
            when(imageService.uploadImageAndGetInfo(any(), eq("products"))).thenReturn(newS3Info);

            try (MockedStatic<GlobalDefaults> mockedDefaults = mockStatic(GlobalDefaults.class)) {
                mockedDefaults.when(() -> GlobalDefaults.isDefaultProductImage(any())).thenReturn(false);

                productService.updateProductImages(1L, List.of(new ImageInfo(null, "keep.jpg", null)), List.of(newFile));

                verify(imageService).deleteFile("discard.jpg");
                assertEquals(2, product.getImages().size());
                List<String> finalKeys = product.getImages().stream().map(ProductImageInfo::getS3Key).toList();
                assertTrue(finalKeys.contains("keep.jpg"));
                assertTrue(finalKeys.contains("new.jpg"));
            }
        }

        @Test
        @DisplayName("updateProductImages with null existingImages deletes all current S3 images")
        void updateProductImages_NullExistingImages_DeletesAllCurrentImages() {
            ProductImageInfo existingImg = new ProductImageInfo();
            existingImg.getImageInfo().setS3Key("old.jpg");
            product.getImages().add(existingImg);

            MultipartFile newFile = new MockMultipartFile("file", "new.jpg", "image/jpeg", new byte[0]);
            ImageInfo newS3Info = new ImageInfo("url", "new.jpg", "new.jpg");

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(userService.getLoggedUser()).thenReturn(Optional.empty());
            when(imageService.uploadImageAndGetInfo(any(), eq("products"))).thenReturn(newS3Info);

            try (MockedStatic<GlobalDefaults> mockedDefaults = mockStatic(GlobalDefaults.class)) {
                mockedDefaults.when(() -> GlobalDefaults.isDefaultProductImage(any())).thenReturn(false);

                productService.updateProductImages(1L, null, List.of(newFile));

                verify(imageService).deleteFile("old.jpg");
                assertEquals(1, product.getImages().size());
                assertEquals("new.jpg", product.getImages().getFirst().getS3Key());
            }
        }

        @Test
        @DisplayName("updateProductImages adds default image when no new files provided and current list is empty after deletions")
        void updateProductImages_NoNewImages_EmptyCurrent_AddsDefaultImage() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(userService.getLoggedUser()).thenReturn(Optional.empty());

            try (MockedStatic<GlobalDefaults> mockedDefaults = mockStatic(GlobalDefaults.class)) {
                ImageInfo defaultImg = new ImageInfo("url", "default.jpg", "default.jpg");
                mockedDefaults.when(GlobalDefaults::getDefaultProductImage).thenReturn(defaultImg);

                productService.updateProductImages(1L, List.of(), null);

                assertEquals(1, product.getImages().size());
                assertEquals(defaultImg, product.getImages().getFirst().getImageInfo());
            }
        }

        @Test
        @DisplayName("updateProductImages removes a discarded default image from the list but does NOT call S3 delete")
        void updateProductImages_DiscardedDefaultImage_NotDeletedFromS3() {
            ProductImageInfo defaultImg = new ProductImageInfo();
            defaultImg.getImageInfo().setS3Key("default.jpg");
            product.getImages().add(defaultImg);

            MultipartFile newFile = new MockMultipartFile("file", "new.jpg", "image/jpeg", new byte[0]);
            ImageInfo newS3Info = new ImageInfo("url", "new.jpg", "new.jpg");

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(userService.getLoggedUser()).thenReturn(Optional.empty());

            try (MockedStatic<GlobalDefaults> mockedDefaults = mockStatic(GlobalDefaults.class)) {
                mockedDefaults.when(() -> GlobalDefaults.isDefaultProductImage(any())).thenReturn(true);
                when(imageService.uploadImageAndGetInfo(any(), eq("products"))).thenReturn(newS3Info);

                // existingImages is empty → the current default image is discarded
                productService.updateProductImages(1L, List.of(), List.of(newFile));

                verify(imageService, never()).deleteFile(any());
                List<String> keys = product.getImages().stream().map(ProductImageInfo::getS3Key).toList();
                assertFalse(keys.contains("default.jpg"), "Discarded default image must be removed from the list");
                assertTrue(keys.contains("new.jpg"), "Newly uploaded image must be present");
            }
        }

        @Test
        @DisplayName("updateProductImages keeps current images whose S3 key is null and never deletes them")
        void updateProductImages_NullS3KeyImage_IsKept() {
            ProductImageInfo nullKeyImg = new ProductImageInfo(); // default ImageInfo → null S3 key
            product.getImages().add(nullKeyImg);

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(userService.getLoggedUser()).thenReturn(Optional.empty());

            productService.updateProductImages(1L, List.of(), null);

            verify(imageService, never()).deleteFile(any());
            assertEquals(1, product.getImages().size(), "Image with a null S3 key must be kept");
            assertSame(nullKeyImg, product.getImages().getFirst());
        }
    }

    // --- PERSONALIZED RECOMMENDATIONS ---
    @Nested
    @DisplayName("Tests for getPersonalizedRecommendations branching logic")
    class PersonalizedRecommendationsTests {

        @Test
        @DisplayName("Anonymous user skips category logic and falls through to views-based and final fallback")
        void getPersonalizedRecommendations_AnonymousUser_UsesFallbacks() {
            when(userService.getLoggedUserUsername()).thenReturn(null);
            when(userService.getLoggedUser()).thenReturn(Optional.empty());
            when(registryService.getTopViewedReferences(anyInt(), any())).thenReturn(List.of("REF-TOP"));
            when(productRepository.findByReferenceCodeIn(List.of("REF-TOP"))).thenReturn(List.of(product));
            when(productRepository.findActiveProductsExcluding(any(), any(Pageable.class))).thenReturn(Page.empty());

            Page<Product> result = productService.getPersonalizedRecommendations(0, 5);

            assertFalse(result.getContent().isEmpty());
            verify(registryService, never()).getInteractedProductReferences(any(), any());
        }

        @Test
        @DisplayName("Anonymous user with empty top-viewed list uses final DB fallback")
        void getPersonalizedRecommendations_AnonymousUser_EmptyTopViewed_UsesFinalFallback() {
            when(userService.getLoggedUserUsername()).thenReturn(null);
            when(userService.getLoggedUser()).thenReturn(Optional.empty());
            when(registryService.getTopViewedReferences(anyInt(), any())).thenReturn(Collections.emptyList());

            Page<Product> fallbackPage = new PageImpl<>(List.of(product));
            when(productRepository.findActiveProductsExcluding(any(), any(Pageable.class))).thenReturn(fallbackPage);

            Page<Product> result = productService.getPersonalizedRecommendations(0, 5);

            verify(productRepository).findActiveProductsExcluding(any(), any(Pageable.class));
            assertFalse(result.getContent().isEmpty());
        }

        @Test
        @DisplayName("Logged user with no prior interactions skips category recs and uses views fallback")
        void getPersonalizedRecommendations_LoggedUser_NoInteractions_UsesViewsFallback() {
            when(userService.getLoggedUserUsername()).thenReturn("test-user");
            when(userService.getLoggedUser()).thenReturn(Optional.empty());
            when(registryService.getInteractedProductReferences(eq("test-user"), anyList()))
                    .thenReturn(Collections.emptySet());
            when(registryService.getTopViewedReferences(anyInt(), any())).thenReturn(List.of("REF-VIEW"));
            when(productRepository.findByReferenceCodeIn(List.of("REF-VIEW"))).thenReturn(List.of(product));
            when(productRepository.findActiveProductsExcluding(any(), any(Pageable.class))).thenReturn(Page.empty());

            Page<Product> result = productService.getPersonalizedRecommendations(0, 5);

            verify(registryService).getTopViewedReferences(anyInt(), any());
            assertFalse(result.getContent().isEmpty());
        }

        @Test
        @DisplayName("Logged user with interactions triggers category-based recommendations (boughtRefs empty → uses DUMMY sentinel)")
        void getPersonalizedRecommendations_LoggedUser_WithInteractions_UsesCategoryRecs() {
            Category cat = new Category();
            cat.setId(1L);
            cat.setChildren(new ArrayList<>());
            product.setCategories(List.of(cat));
            product.setReferenceCode("REF-INT");

            // First call (interestActions) returns interactions; second call (USER_ORDERS) returns empty boughtRefs
            when(userService.getLoggedUserUsername()).thenReturn("test-user");
            when(userService.getLoggedUser()).thenReturn(Optional.empty());
            when(registryService.getInteractedProductReferences(eq("test-user"), anyList()))
                    .thenReturn(Set.of("REF-INT"))
                    .thenReturn(Collections.emptySet()); // boughtRefs is empty → "DUMMY" sentinel used

            when(productRepository.findByReferenceCodeIn(anyCollection())).thenReturn(List.of(product));

            Page<Product> catPage = new PageImpl<>(List.of(product));
            when(productRepository.findRecommendedProducts(anyList(), any(), any(Pageable.class))).thenReturn(catPage);

            Page<Product> result = productService.getPersonalizedRecommendations(0, 1);

            verify(productRepository).findRecommendedProducts(anyList(), any(), any(Pageable.class));
            assertEquals(1, result.getContent().size());
        }

        @Test
        @DisplayName("Logged user with interactions but all parent-category products yields empty topCategoryIds → skips findRecommendedProducts")
        void getPersonalizedRecommendations_LoggedUser_InteractionsWithNoLeafCategories_SkipsCategoryRecs() {
            // Product has no categories → topCategoryIds will be empty → skip findRecommendedProducts
            product.setCategories(new ArrayList<>());
            product.setReferenceCode("REF-INT2");

            when(userService.getLoggedUserUsername()).thenReturn("test-user");
            when(registryService.getInteractedProductReferences(eq("test-user"), anyList()))
                    .thenReturn(Set.of("REF-INT2"));
            when(productRepository.findByReferenceCodeIn(anyCollection())).thenReturn(List.of(product));
            when(registryService.getTopViewedReferences(anyInt(), any())).thenReturn(Collections.emptyList());
            when(productRepository.findActiveProductsExcluding(any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            productService.getPersonalizedRecommendations(0, 5);

            verify(productRepository, never()).findRecommendedProducts(anyList(), any(), any(Pageable.class));
        }
    }
}
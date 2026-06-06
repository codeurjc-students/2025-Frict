package com.tfg.backend.integration;

import com.tfg.backend.dto.ProductDTO;
import com.tfg.backend.dto.ProductSpecDTO;
import com.tfg.backend.dto.SpecFilterDTO;
import com.tfg.backend.model.*;
import com.tfg.backend.repository.*;
import com.tfg.backend.service.ImageService;
import com.tfg.backend.service.ProductService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests class for the ProductService and ProductRepository.
 * Validates complex product logic including stock enrichment, cart item removal on deactivation,
 * category fallbacks, and safe deletions preserving historical order data in MySQL.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ProductServiceITest {

    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private ShopStockRepository shopStockRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private EntityManager entityManager;

    @MockitoBean private ImageService imageService;

    private User activeUser;
    private Shop mainShop;
    private Category categoryOtros;
    private Category categoryTech;
    private Product productLaptop;
    private Product productPhone;
    private OrderItem cartItemPhone;
    private OrderItem purchasedItemLaptop;

    @BeforeEach
    void setUpComplexProductScenario() {
        // 1. Create Categories (Otros is mandatory for fallback)
        categoryOtros = new Category("Otros", "icon", "banner", "short", "long");
        categoryTech = new Category("Tech", "icon", "banner", "short", "long");
        categoryRepository.saveAll(List.of(categoryOtros, categoryTech));

        // 2. Create Shop
        mainShop = new Shop();
        mainShop.setName("Main Tech Shop");
        mainShop.setReferenceCode("SHOP-PROD-123");
        mainShop.setAssignedBudget(10000.0);
        shopRepository.save(mainShop);

        // 3. Create User with role USER and assign to the shop
        activeUser = new User("John", "john_doe", "john@mail.com", "pass", "USER");
        activeUser.setSelectedShop(mainShop);
        userRepository.save(activeUser);

        // Authenticate User in SecurityContext (Needs authorities to pass hasRole("USER") check)
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(activeUser.getUsername(), "pass", List.of(new SimpleGrantedAuthority("USER"))));

        // 4. Create Products
        productLaptop = new Product("Gaming Laptop", "High performance", 1500.0, 1000.0);
        productLaptop.setReferenceCode("PROD-LAPTOP-1");
        productLaptop.getCategories().add(categoryTech);

        // Setup bidirectional dummy image for Laptop
        ProductImageInfo imgLaptop = new ProductImageInfo(new ImageInfo("laptop.jpg", "url", "laptop.jpg"), productLaptop);
        productLaptop.getImages().add(imgLaptop);

        productPhone = new Product("Smartphone XYZ", "Latest model", 800.0, 500.0);
        productPhone.setReferenceCode("PROD-PHONE-1");
        productPhone.getCategories().add(categoryTech);

        // Setup bidirectional dummy image for Phone
        ProductImageInfo imgPhone = new ProductImageInfo(new ImageInfo("phone.jpg", "url", "phone.jpg"), productPhone);
        productPhone.getImages().add(imgPhone);

        productRepository.saveAll(List.of(productLaptop, productPhone));

        // 5. Create Stock ONLY for Laptop in the Main Shop
        ShopStock laptopStock = new ShopStock(mainShop, productLaptop, 5);
        shopStockRepository.save(laptopStock);

        // 6. Create Orders and OrderItems
        // A. Phone is in the User's Cart (Order = null)
        cartItemPhone = new OrderItem(productPhone, activeUser, 1);
        orderItemRepository.save(cartItemPhone);

        // B. Laptop is already purchased in a Past Order
        Order pastOrder = new Order();
        pastOrder.setUser(activeUser);
        pastOrder.setReferenceCode("ORD-PAST-1");
        orderRepository.save(pastOrder);

        purchasedItemLaptop = new OrderItem(productLaptop, activeUser, 1);
        purchasedItemLaptop.setOrder(pastOrder); // Mark as purchased
        orderItemRepository.save(purchasedItemLaptop);

        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Repository: findProductsNotAssignedToShop correctly identifies products with zero stock entries")
    void testFindProductsNotAssignedToShop() {
        // Laptop has stock in mainShop, Phone does not.
        List<Product> unassigned = productService.findProductsNotAssignedToShop(mainShop.getId());

        assertEquals(1, unassigned.size(), "Only the Phone should be returned");
        assertEquals("Smartphone XYZ", unassigned.getFirst().getName());
    }

    @Test
    @DisplayName("Repository: getFilteredProducts retrieves products accurately by name matching and category")
    void testFindByFilters() {
        // Search by partial text "smart" and category Tech
        Page<Product> result = productService.getFilteredProducts("smart", List.of(categoryTech.getId()), List.of(), PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Smartphone XYZ", result.getContent().getFirst().getName());
    }

    @Test
    @DisplayName("Create Product: Missing categories trigger automatic fallback to 'Otros' category")
    void testCreateProduct_NoCategories_FallsBackToOtros() {
        ProductDTO dto = new ProductDTO();
        dto.setName("Generic Cable");
        dto.setDescription("Just a cable");
        dto.setSupplyPrice(5.0);
        dto.setCurrentPrice(10.0);
        dto.setCategories(new ArrayList<>()); // Empty categories

        Product created = productService.createProduct(dto);

        entityManager.flush();
        entityManager.clear();

        Product dbProduct = productRepository.findById(created.getId()).orElseThrow();
        assertEquals(1, dbProduct.getCategories().size(), "Should have exactly 1 category assigned");
        assertEquals("Otros", dbProduct.getCategories().getFirst().getName(), "Should fallback to 'Otros'");
    }

    @Test
    @DisplayName("Favorites: Add and remove product from user's favourite list properly updates DB")
    void testFavouritesManagement() {
        // Add to favourites
        productService.addProductToFavourites(productPhone.getId());

        entityManager.flush();
        entityManager.clear();

        User userAfterAdd = userRepository.findById(activeUser.getId()).orElseThrow();
        assertTrue(userAfterAdd.getFavouriteProducts().stream().anyMatch(p -> p.getId().equals(productPhone.getId())));

        // Remove from favourites
        productService.deleteProductFromFavourites(productPhone.getId());

        entityManager.flush();
        entityManager.clear();

        User userAfterRemove = userRepository.findById(activeUser.getId()).orElseThrow();
        assertTrue(userAfterRemove.getFavouriteProducts().isEmpty());
    }

    @Test
    @DisplayName("Toggle Activation: Deactivating a product removes it from shopping carts but keeps purchased history")
    void testToggleActivation_RemovesFromCarts_KeepsInPastOrders() {
        // Deactivate Laptop (has a purchased item)
        productService.toggleGlobalActivation(productLaptop.getId(), false);

        // Deactivate Phone (has a cart item)
        productService.toggleGlobalActivation(productPhone.getId(), false);

        entityManager.flush();
        entityManager.clear();

        // Check Phone (Cart Item) -> Should be deleted
        Optional<OrderItem> deletedCartItem = orderItemRepository.findById(cartItemPhone.getId());
        assertTrue(deletedCartItem.isEmpty(), "Cart item should be deleted because the product was deactivated");

        // Check Laptop (Purchased Item) -> Should remain intact
        Optional<OrderItem> keptPurchasedItem = orderItemRepository.findById(purchasedItemLaptop.getId());
        assertTrue(keptPurchasedItem.isPresent(), "Purchased items must NOT be deleted when a product is deactivated");
    }

    @Test
    @DisplayName("Create Product: Specifications are persisted in DB with correct names and values")
    void testCreateProduct_WithSpecs_PersistsSpecs() {
        ProductDTO dto = new ProductDTO();
        dto.setName("Spec Product");
        dto.setSupplyPrice(20.0);
        dto.setCurrentPrice(30.0);
        dto.setCategories(new ArrayList<>());
        dto.setSpecifications(List.of(
                new ProductSpecDTO(null, "Color", List.of("Rojo", "Azul")),
                new ProductSpecDTO(null, "Talla", List.of("M", "XL"))
        ));

        Product created = productService.createProduct(dto);

        entityManager.flush();
        entityManager.clear();

        Product dbProduct = productRepository.findById(created.getId()).orElseThrow();
        assertEquals(2, dbProduct.getSpecifications().size());

        ProductSpec colorSpec = dbProduct.getSpecifications().stream()
                .filter(s -> s.getName().equals("Color")).findFirst().orElseThrow();
        assertTrue(colorSpec.getValues().containsAll(List.of("Rojo", "Azul")));
    }

    @Test
    @DisplayName("Update Product: Existing specifications are fully replaced by the new ones from DTO")
    void testUpdateProduct_WithSpecs_ReplacesSpecs() {
        ProductDTO createDto = new ProductDTO();
        createDto.setName("Spec Update Test");
        createDto.setSupplyPrice(10.0);
        createDto.setCurrentPrice(20.0);
        createDto.setCategories(new ArrayList<>());
        createDto.setSpecifications(List.of(new ProductSpecDTO(null, "Color", List.of("Verde"))));
        Product created = productService.createProduct(createDto);

        entityManager.flush();
        entityManager.clear();

        ProductDTO updateDto = new ProductDTO();
        updateDto.setName("Spec Update Test");
        updateDto.setSupplyPrice(10.0);
        updateDto.setCurrentPrice(20.0);
        updateDto.setCategories(new ArrayList<>());
        updateDto.setSpecifications(List.of(new ProductSpecDTO(null, "Talla", List.of("S", "M"))));
        productService.updateProduct(created.getId(), updateDto);

        entityManager.flush();
        entityManager.clear();

        Product dbProduct = productRepository.findById(created.getId()).orElseThrow();
        assertEquals(1, dbProduct.getSpecifications().size());
        assertEquals("Talla", dbProduct.getSpecifications().getFirst().getName());
        assertTrue(dbProduct.getSpecifications().stream().noneMatch(s -> s.getName().equals("Color")),
                "Old 'Color' spec must be replaced by orphanRemoval");
    }

    @Test
    @DisplayName("Repository: getFilteredProducts with spec filter returns only products whose spec matches")
    void testGetFilteredProducts_BySpec_FiltersCorrectly() {
        ProductDTO dtoRojo = new ProductDTO();
        dtoRojo.setName("Producto Rojo");
        dtoRojo.setSupplyPrice(10.0);
        dtoRojo.setCurrentPrice(20.0);
        dtoRojo.setCategories(new ArrayList<>());
        dtoRojo.setSpecifications(List.of(new ProductSpecDTO(null, "Color", List.of("Rojo"))));
        Product prodRojo = productService.createProduct(dtoRojo);

        ProductDTO dtoAzul = new ProductDTO();
        dtoAzul.setName("Producto Azul");
        dtoAzul.setSupplyPrice(10.0);
        dtoAzul.setCurrentPrice(20.0);
        dtoAzul.setCategories(new ArrayList<>());
        dtoAzul.setSpecifications(List.of(new ProductSpecDTO(null, "Color", List.of("Azul"))));
        productService.createProduct(dtoAzul);

        entityManager.flush();
        entityManager.clear();

        List<SpecFilterDTO> filters = List.of(new SpecFilterDTO("Color", List.of("Rojo")));
        Page<Product> result = productService.getFilteredProducts(null, null, filters, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements(), "Only the product with Color=Rojo should match");
        assertEquals(prodRojo.getId(), result.getContent().getFirst().getId());
    }

    @Test
    @DisplayName("getSpecsCatalog returns all distinct spec names and their values from DB")
    void testGetSpecsCatalog_ReturnsKnownSpecs() {
        ProductDTO dto = new ProductDTO();
        dto.setName("Catalogo Test");
        dto.setSupplyPrice(5.0);
        dto.setCurrentPrice(10.0);
        dto.setCategories(new ArrayList<>());
        dto.setSpecifications(List.of(
                new ProductSpecDTO(null, "Color", List.of("Rojo", "Azul")),
                new ProductSpecDTO(null, "Talla", List.of("M"))
        ));
        productService.createProduct(dto);

        entityManager.flush();
        entityManager.clear();

        Map<String, List<String>> catalog = productService.getSpecsCatalog();

        assertTrue(catalog.containsKey("Color"), "Catalog must include 'Color'");
        assertTrue(catalog.get("Color").containsAll(List.of("Rojo", "Azul")));
        assertTrue(catalog.containsKey("Talla"), "Catalog must include 'Talla'");
    }

    @Test
    @DisplayName("Delete Product: Completely removes product but unlinks it from purchased items safely")
    void testDeleteProduct_UnlinksFromOrderItems() {
        // Act: Delete the Laptop
        productService.deleteProduct(productLaptop.getId());

        entityManager.flush();
        entityManager.clear();

        // Assert Product is gone
        assertFalse(productRepository.existsById(productLaptop.getId()));

        // Assert the OrderItem is kept but unlinked
        OrderItem historicalItem = orderItemRepository.findById(purchasedItemLaptop.getId()).orElseThrow();
        assertNull(historicalItem.getProduct(), "The product reference in the historical OrderItem must be nullified to preserve the invoice");
    }
}
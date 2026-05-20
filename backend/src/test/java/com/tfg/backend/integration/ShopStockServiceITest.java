package com.tfg.backend.integration;

import com.tfg.backend.model.Product;
import com.tfg.backend.model.Shop;
import com.tfg.backend.model.ShopStock;
import com.tfg.backend.repository.ProductRepository;
import com.tfg.backend.repository.ShopRepository;
import com.tfg.backend.repository.ShopStockRepository;
import com.tfg.backend.service.ShopStockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ShopStockService.
 * Focuses on stock availability, multi-product stock retrieval,
 * and active status filtering directly in the MySQL database.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ShopStockServiceITest {

    @Autowired private ShopStockService shopStockService;
    @Autowired private ShopStockRepository shopStockRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private ProductRepository productRepository;

    private Shop mainShop;
    private Shop secondaryShop;
    private Product laptop;
    private Product smartphone;

    @BeforeEach
    void setUpStockScenario() {
        // 1. Create and save Shops
        mainShop = new Shop("Main Shop", null, 10000.0);
        mainShop.setReferenceCode("SHOP-STOCK-1");
        secondaryShop = new Shop("Secondary Shop", null, 5000.0);
        secondaryShop.setReferenceCode("SHOP-STOCK-2");
        shopRepository.saveAll(Arrays.asList(mainShop, secondaryShop));

        // 2. Create and save Products
        laptop = new Product("Laptop", "High end", 1000.0, 700.0);
        laptop.setReferenceCode("PROD-LAP-1");
        smartphone = new Product("Smartphone", "Latest Gen", 800.0, 500.0);
        smartphone.setReferenceCode("PROD-PHO-1");
        productRepository.saveAll(Arrays.asList(laptop, smartphone));

        // 3. Create Stock entries
        // Main Shop has 10 Laptops (Active) and 5 Smartphones (Inactive)
        ShopStock s1 = new ShopStock(mainShop, laptop, 10);
        s1.setActive(true);

        ShopStock s2 = new ShopStock(mainShop, smartphone, 5);
        s2.setActive(false); // Should be ignored by stock queries

        // Secondary Shop has 20 Smartphones (Active)
        ShopStock s3 = new ShopStock(secondaryShop, smartphone, 20);
        s3.setActive(true);

        shopStockRepository.saveAll(Arrays.asList(s1, s2, s3));
    }

    @Test
    @DisplayName("findUnitsByProductIdAndShopId returns correct units only if stock is active")
    void testFindUnitsByProductIdAndShopId_ActiveFiltering() {
        // Test Active Stock
        Optional<Integer> laptopStock = shopStockRepository.findUnitsByProductIdAndShopId(laptop.getId(), mainShop.getId());

        // Test Inactive Stock (Smartphone in Main Shop is inactive)
        Optional<Integer> phoneStock = shopStockRepository.findUnitsByProductIdAndShopId(smartphone.getId(), mainShop.getId());

        assertAll(
                () -> assertTrue(laptopStock.isPresent()),
                () -> assertEquals(10, laptopStock.get(), "Should return 10 units for the active laptop stock"),
                () -> assertTrue(phoneStock.isEmpty(), "Should return empty for inactive stock even if units exist in DB")
        );
    }

    @Test
    @DisplayName("getLocalStocks retrieves a list of stock units preserving the input order")
    void testGetLocalStocks_ListRetrieval() {
        List<Product> queryList = Arrays.asList(laptop, smartphone);

        // Query stock for Main Shop
        List<Integer> mainShopStocks = shopStockService.getLocalStocks(queryList, mainShop.getId());

        assertAll(
                () -> assertEquals(2, mainShopStocks.size()),
                () -> assertEquals(10, mainShopStocks.getFirst(), "First element (Laptop) should have 10 units"),
                () -> assertEquals(-1, mainShopStocks.get(1), "Second element (Smartphone) should be -1 because it's inactive")
        );
    }

    @Test
    @DisplayName("getLocalStocks returns 0 for products with no stock entry in the specified shop")
    void testGetLocalStocks_NoEntryReturnsZero() {
        List<Product> queryList = List.of(laptop);

        // Query stock for Secondary Shop (Laptop is not assigned to Secondary Shop)
        List<Integer> secondaryShopStocks = shopStockService.getLocalStocks(queryList, secondaryShop.getId());

        assertAll(
                () -> assertEquals(1, secondaryShopStocks.size()),
                () -> assertEquals(0, secondaryShopStocks.getFirst(), "Should return 0 if no ShopStock entry exists for that product in the shop")
        );
    }

    @Test
    @DisplayName("save and deleteById update the database state correctly")
    void testSaveAndDeleteStock() {
        // Create new stock
        ShopStock newStock = new ShopStock(secondaryShop, laptop, 15);
        ShopStock saved = shopStockService.save(newStock);

        assertTrue(shopStockRepository.existsById(saved.getId()));

        // Delete stock
        shopStockService.deleteById(saved.getId());

        assertFalse(shopStockRepository.existsById(saved.getId()), "Stock should be deleted from DB");
    }

    @Test
    @DisplayName("getLocalStock (Single Product) returns null if shopId or product is null")
    void testGetLocalStock_NullHandling() {
        Integer result = shopStockService.getLocalStock(laptop, null);
        assertNull(result, "Should return null if shopId is not provided");
    }
}
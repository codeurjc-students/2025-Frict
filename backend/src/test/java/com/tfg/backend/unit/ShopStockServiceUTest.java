package com.tfg.backend.unit;

import com.tfg.backend.model.Product;
import com.tfg.backend.model.ShopStock;
import com.tfg.backend.repository.ShopStockRepository;
import com.tfg.backend.service.ShopStockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShopStockServiceUTest {

    @Mock
    private ShopStockRepository shopStockRepository;

    @InjectMocks
    private ShopStockService shopStockService;

    private Product p1;
    private Product p2;
    private Product p3;
    private ShopStock stock1;
    private ShopStock stock3;

    @BeforeEach
    void setUp() {
        p1 = new Product();
        p1.setId(10L);

        p2 = new Product();
        p2.setId(20L);

        p3 = new Product();
        p3.setId(30L);

        stock1 = new ShopStock();
        stock1.setId(100L);
        stock1.setProduct(p1);
        stock1.setUnits(5);

        stock3 = new ShopStock();
        stock3.setId(300L);
        stock3.setProduct(p3);
        stock3.setUnits(12);
    }

    // --- HELPER TESTS ---
    @Nested
    @DisplayName("Tests for findShopStockHelper")
    class HelperTests {

        @Test
        @DisplayName("Returns ShopStock successfully when found")
        void findShopStockHelper_Success() {
            when(shopStockRepository.findById(100L)).thenReturn(Optional.of(stock1));

            ShopStock result = shopStockService.findShopStockHelper(100L);

            assertNotNull(result);
            assertEquals(100L, result.getId());
        }

        @Test
        @DisplayName("Throws 404 NOT_FOUND when ShopStock is missing")
        void findShopStockHelper_ThrowsNotFound() {
            when(shopStockRepository.findById(999L)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> shopStockService.findShopStockHelper(999L));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertEquals("Stock with ID 999 does not exist.", ex.getReason());
        }
    }

    // --- SINGLE STOCK FETCHING TESTS ---
    @Nested
    @DisplayName("Tests for getLocalStock (Single Product)")
    class GetSingleLocalStockTests {

        @Test
        @DisplayName("Returns null if shopId or product is null")
        void getLocalStock_ReturnsNull_WhenInvalidInputs() {
            assertNull(shopStockService.getLocalStock(null, 1L), "Should return null if product is null");
            assertNull(shopStockService.getLocalStock(p1, null), "Should return null if shopId is null");
        }

        @Test
        @DisplayName("Returns exact units if product is found in the shop and active")
        void getLocalStock_ReturnsUnits_WhenFound() {
            ShopStock activeStock = new ShopStock();
            activeStock.setProduct(p1);
            activeStock.setUnits(42);
            activeStock.setActive(true);
            when(shopStockRepository.findByProduct_IdAndShop_Id(10L, 5L)).thenReturn(Optional.of(activeStock));

            Integer units = shopStockService.getLocalStock(p1, 5L);

            assertEquals(42, units);
        }

        @Test
        @DisplayName("Returns -1 if product stock exists in the shop but is inactive")
        void getLocalStock_ReturnsMinusOne_WhenInactive() {
            ShopStock inactiveStock = new ShopStock();
            inactiveStock.setProduct(p1);
            inactiveStock.setUnits(10);
            inactiveStock.setActive(false);
            when(shopStockRepository.findByProduct_IdAndShop_Id(10L, 5L)).thenReturn(Optional.of(inactiveStock));

            Integer units = shopStockService.getLocalStock(p1, 5L);

            assertEquals(-1, units);
        }

        @Test
        @DisplayName("Returns null if product has no stock record in the shop")
        void getLocalStock_ReturnsNull_WhenNotFound() {
            when(shopStockRepository.findByProduct_IdAndShop_Id(10L, 5L)).thenReturn(Optional.empty());

            Integer units = shopStockService.getLocalStock(p1, 5L);

            assertNull(units);
        }
    }

    // --- BATCH STOCK FETCHING TESTS (List & Page) ---
    @Nested
    @DisplayName("Tests for getLocalStocks (List and Page)")
    class GetBatchLocalStocksTests {

        @Test
        @DisplayName("Returns list of nulls if shopId is null")
        void getLocalStocks_ReturnsNulls_WhenShopIdIsNull() {
            List<Product> products = List.of(p1, p2);

            List<Integer> result = shopStockService.getLocalStocks(products, null);

            assertEquals(2, result.size());
            assertNull(result.get(0));
            assertNull(result.get(1));
            verifyNoInteractions(shopStockRepository);
        }

        @Test
        @DisplayName("Returns empty list if product list is empty")
        void getLocalStocks_ReturnsEmptyList_WhenProductsEmpty() {
            List<Integer> result = shopStockService.getLocalStocks(Collections.emptyList(), 5L);

            assertTrue(result.isEmpty());
            verifyNoInteractions(shopStockRepository);
        }

        @Test
        @DisplayName("Maps units correctly preserving the original product order and defaulting to 0 for missing stock")
        void getLocalStocks_List_MapsCorrectly() {
            List<Product> products = List.of(p1, p2, p3);

            // DB returns stock for p1 and p3. p2 has no stock record.
            when(shopStockRepository.findStockForProductsInShop(eq(5L), anyList()))
                    .thenReturn(List.of(stock1, stock3));

            List<Integer> result = shopStockService.getLocalStocks(products, 5L);

            assertEquals(3, result.size());
            assertEquals(5, result.get(0), "Stock for p1 should be 5");
            assertEquals(0, result.get(1), "Stock for p2 should default to 0");
            assertEquals(12, result.get(2), "Stock for p3 should be 12");
        }

        @Test
        @DisplayName("getLocalStocks with Page extracts content and delegates to List method")
        void getLocalStocks_Page_DelegatesToList() {
            List<Product> products = List.of(p1, p3);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> productPage = new PageImpl<>(products, pageable, 2);

            when(shopStockRepository.findStockForProductsInShop(eq(5L), anyList()))
                    .thenReturn(List.of(stock1, stock3));

            List<Integer> result = shopStockService.getLocalStocks(productPage, 5L);

            assertEquals(2, result.size());
            assertEquals(5, result.get(0));
            assertEquals(12, result.get(1));
        }
    }
}
package com.tfg.backend.unit;

import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.dto.ShopDTO;
import com.tfg.backend.model.*;
import com.tfg.backend.repository.ShopRepository;
import com.tfg.backend.service.ImageService;
import com.tfg.backend.service.ProductService;
import com.tfg.backend.service.ShopService;
import com.tfg.backend.service.ShopStockService;
import com.tfg.backend.utils.GlobalDefaults;
import com.tfg.backend.utils.StatDTO;
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
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopServiceUTest {

    @Mock private ImageService imageService;
    @Mock private ShopStockService shopStockService;
    @Mock private ProductService productService;
    @Mock private ShopRepository shopRepository;
    @Mock private ApplicationEventPublisher eventPublisher; //Necessary to avoid errors trying to send notifications, but not used

    @InjectMocks
    private ShopService shopService;

    private Shop shop;
    private ShopDTO shopDTO;
    private AddressDTO addressDTO;

    @BeforeEach
    void setUp() {
        // Setup real Shop entity
        shop = new Shop();
        shop.setId(1L);
        shop.setName("Test Shop");
        shop.setAssignedBudget(1000.0);
        shop.setAssignedTrucks(new ArrayList<>());
        shop.setAssignedOrders(new ArrayList<>());
        shop.setCustomers(new ArrayList<>());

        ImageInfo img = new ImageInfo();
        img.setS3Key("shop-pic.jpg");
        shop.setImage(img);

        // Setup DTOs
        addressDTO = new AddressDTO();
        addressDTO.setAlias("Main Store");
        addressDTO.setStreet("Gran Via");
        addressDTO.setNumber("12");
        addressDTO.setCity("Madrid");
        addressDTO.setLatitude(40.4168);
        addressDTO.setLongitude(-3.7038);

        shopDTO = new ShopDTO();
        shopDTO.setName("New Shop Name");
        shopDTO.setAssignedBudget(5000.0);
        shopDTO.setAddress(addressDTO);
    }

    // --- HELPER TESTS ---
    @Nested
    @DisplayName("Tests for internal helper methods")
    class HelperTests {

        @Test
        @DisplayName("findShopHelper returns Shop if it exists")
        void findShopHelper_Success() {
            when(shopRepository.findById(1L)).thenReturn(Optional.of(shop));

            Shop result = shopService.findShopHelper(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("findShopHelper throws 404 NOT_FOUND if shop is missing")
        void findShopHelper_ThrowsNotFound() {
            when(shopRepository.findById(1L)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> shopService.findShopHelper(1L));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertEquals("Shop with ID 1 does not exist.", ex.getReason());
        }
    }

    // --- CRUD TESTS ---
    @Nested
    @DisplayName("Tests for basic Shop creation and updates")
    class CrudTests {

        @Test
        @DisplayName("createShop maps DTO properties to a new Entity and saves it")
        void createShop_Success() {
            when(shopRepository.save(any(Shop.class))).thenAnswer(i -> i.getArgument(0));

            Shop result = shopService.createShop(shopDTO);

            assertEquals("New Shop Name", result.getName());
            assertEquals(5000.0, result.getAssignedBudget());
            assertNotNull(result.getAddress());
            assertEquals("Main Store", result.getAddress().getAlias());
            assertEquals(40.4168, result.getAddress().getLatitude());

            verify(shopRepository).save(any(Shop.class));
        }

        @Test
        @DisplayName("updateShop updates existing entity fields using DTO data")
        void updateShop_Success() {
            when(shopRepository.findById(1L)).thenReturn(Optional.of(shop));

            Shop result = shopService.updateShop(1L, shopDTO);

            assertEquals("New Shop Name", result.getName());
            assertEquals(5000.0, result.getAssignedBudget());
            assertEquals("Madrid", result.getAddress().getCity());
        }

        @Test
        @DisplayName("deleteShop unlinks memory collections, cancels active orders and deletes S3 image")
        void deleteShop_CascadesAndUnlinks() {
            // Setup Truck
            Truck truck = new Truck();
            truck.setAssignedShop(shop);
            shop.getAssignedTrucks().add(truck);

            // Setup Customer
            User customer = new User();
            customer.setSelectedShop(shop);
            shop.getCustomers().add(customer);

            // Setup Orders (One active, one completed)
            Order activeOrder = new Order();
            activeOrder.changeOrderStatus(OrderStatus.ORDER_MADE, "Init");
            activeOrder.setAssignedShop(shop);

            Order completedOrder = new Order();
            completedOrder.changeOrderStatus(OrderStatus.COMPLETED, "Done");
            completedOrder.setAssignedShop(shop);

            shop.getAssignedOrders().addAll(List.of(activeOrder, completedOrder));

            when(shopRepository.findById(1L)).thenReturn(Optional.of(shop));

            try (MockedStatic<GlobalDefaults> mockedDefaults = mockStatic(GlobalDefaults.class)) {
                mockedDefaults.when(() -> GlobalDefaults.isDefaultShopImage(shop.getImage())).thenReturn(false);

                // Act
                shopService.deleteShop(1L);

                // Assert unlinking
                assertTrue(shop.getAssignedTrucks().isEmpty());
                assertNull(truck.getAssignedShop());

                assertTrue(shop.getCustomers().isEmpty());
                assertNull(customer.getSelectedShop());

                assertTrue(shop.getAssignedOrders().isEmpty());
                assertNull(activeOrder.getAssignedShop());
                assertNull(completedOrder.getAssignedShop());

                // Assert business logic: Active order must be cancelled
                assertEquals(OrderStatus.CANCELLED, activeOrder.getCurrentStatus());
                assertEquals("La tienda a la que estaba asignado el pedido ha sido eliminada.",
                        activeOrder.getHistory().getLast().getUpdates().getLast().getDescription());

                // Completed order must remain completed
                assertEquals(OrderStatus.COMPLETED, completedOrder.getCurrentStatus());

                // Assert deletions
                verify(imageService).deleteFile("shop-pic.jpg");
                verify(shopRepository).delete(shop);
            }
        }
    }

    // --- STOCK MANAGEMENT TESTS ---
    @Nested
    @DisplayName("Tests for Stock Activation and Assignment")
    class StockManagementTests {

        @Test
        @DisplayName("toggleLocalActivation changes stock activation state")
        void toggleLocalActivation_Success() {
            ShopStock stock = new ShopStock();
            stock.setActive(true);
            stock.setShop(shop); //Necessary for notifications logic

            when(shopStockService.findShopStockHelper(5L)).thenReturn(stock);

            ShopStock result = shopService.toggleLocalActivation(5L, false);

            assertFalse(result.isActive());
        }

        @Test
        @DisplayName("toggleAllLocalActivations changes state for all shop's stocks")
        void toggleAllLocalActivations_Success() {
            ShopStock s1 = new ShopStock(); s1.setActive(true);
            ShopStock s2 = new ShopStock(); s2.setActive(true);

            when(shopStockService.findAllByShopId(1L)).thenReturn(List.of(s1, s2));

            boolean finalState = shopService.toggleAllLocalActivations(1L, false);

            assertFalse(finalState);
            assertFalse(s1.isActive());
            assertFalse(s2.isActive());
        }

        @Test
        @DisplayName("setAssignedStock creates new stock when state is true")
        void setAssignedStock_CreatesStock_WhenTrue() {
            Product product = new Product();
            product.setId(99L);

            when(shopRepository.findById(1L)).thenReturn(Optional.of(shop));
            when(productService.findProductHelper(99L)).thenReturn(product);
            when(shopStockService.save(any(ShopStock.class))).thenAnswer(i -> i.getArgument(0));

            ShopStock result = shopService.setAssignedStock(1L, 99L, true);

            assertNotNull(result);
            assertEquals(shop, result.getShop());
            assertEquals(product, result.getProduct());
            assertEquals(0, result.getUnits(), "New stock must be initialized with 0 units");

            verify(shopStockService).save(any(ShopStock.class));
            verify(shopStockService, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("setAssignedStock deletes existing stock when state is false")
        void setAssignedStock_DeletesStock_WhenFalse() {
            ShopStock existingStock = new ShopStock();
            when(shopRepository.findById(1L)).thenReturn(Optional.of(shop));
            when(shopStockService.findShopStockHelper(99L)).thenReturn(existingStock); // Stock ID 99

            ShopStock result = shopService.setAssignedStock(1L, 99L, false);

            assertEquals(existingStock, result);
            verify(shopStockService).deleteById(99L);
            verify(shopStockService, never()).save(any(ShopStock.class));
        }
    }

    // --- RESTOCKING TESTS ---
    @Nested
    @DisplayName("Tests for restockProduct logic")
    class RestockProductTests {
        private Product product;
        private ShopStock stock;

        @BeforeEach
        void setupStock() {
            product = new Product();
            product.setSupplyPrice(10.0); // Cost per unit

            stock = new ShopStock();
            stock.setShop(shop); // Shop has 1000.0 budget
            stock.setProduct(product);
            stock.setUnits(5);
        }

        @Test
        @DisplayName("Throws BAD_REQUEST if restock units are 0 or negative")
        void restockProduct_ThrowsBadRequest_WhenUnitsNegative() {
            when(shopStockService.findShopStockHelper(5L)).thenReturn(stock);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> shopService.restockProduct(5L, 0));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        @DisplayName("Throws FORBIDDEN if shop budget is insufficient for the restock operation")
        void restockProduct_ThrowsForbidden_WhenBudgetInsufficient() {
            when(shopStockService.findShopStockHelper(5L)).thenReturn(stock);

            // Attempting to restock 200 units * 10.0 cost = 2000.0 (Budget is only 1000.0)
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> shopService.restockProduct(5L, 200));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            assertEquals("There is not enough budget in this shop to complete this operation.", ex.getReason());
        }

        @Test
        @DisplayName("Successfully increases stock units and decreases shop budget")
        void restockProduct_Success() {
            when(shopStockService.findShopStockHelper(5L)).thenReturn(stock);

            // Restocking 20 units * 10.0 cost = 200.0 cost
            ShopStock result = shopService.restockProduct(5L, 20);

            // Assert Stock modification
            assertEquals(25, result.getUnits()); // 5 existing + 20 new

            // Assert Shop Budget modification
            assertEquals(800.0, shop.getAssignedBudget()); // 1000.0 original - 200.0 cost
        }
    }

    // --- IMAGE HANDLING TESTS ---
    @Nested
    @DisplayName("Tests for Shop Image Handling")
    class ImageHandlingTests {

        @Test
        @DisplayName("uploadShopImage delegates to ImageService and updates image")
        void uploadShopImage_Success() {
            MultipartFile mockFile = new MockMultipartFile("file", "new.jpg", "image/jpeg", new byte[0]);
            ImageInfo newS3Info = new ImageInfo();
            newS3Info.setS3Key("new.jpg");

            when(shopRepository.findById(1L)).thenReturn(Optional.of(shop));
            when(imageService.processImageReplacement(any(), eq(mockFile), eq("shops"), any(), any()))
                    .thenReturn(newS3Info);

            Shop result = shopService.uploadShopImage(1L, mockFile);

            assertEquals(newS3Info, result.getImage());
        }

        @Test
        @DisplayName("deleteShopImage deletes S3 file and applies default if image is custom")
        void deleteShopImage_Success() {
            when(shopRepository.findById(1L)).thenReturn(Optional.of(shop)); // shop has "shop-pic.jpg"

            try (MockedStatic<GlobalDefaults> mockedDefaults = mockStatic(GlobalDefaults.class)) {
                mockedDefaults.when(() -> GlobalDefaults.isDefaultShopImage(shop.getImage())).thenReturn(false);

                ImageInfo defaultImg = new ImageInfo();
                mockedDefaults.when(GlobalDefaults::getDefaultShopImage).thenReturn(defaultImg);

                Shop result = shopService.deleteShopImage(1L);

                verify(imageService).deleteFile("shop-pic.jpg");
                assertEquals(defaultImg, result.getImage());
            }
        }
    }

    // --- METRICS TESTS ---
    @Nested
    @DisplayName("Tests for Shop Statistics")
    class MetricsTests {

        @Test
        @DisplayName("Returns correct metrics for ADMIN role fetching global data")
        void getShopsStatistics_ForAdmin() {
            User admin = new User();
            admin.setRoles(new java.util.HashSet<>(List.of("ADMIN")));

            when(shopRepository.count()).thenReturn(5L);
            when(shopRepository.sumAllAssignedBudgets()).thenReturn(20000.0);

            List<StatDTO> stats = shopService.getShopsStatistics(admin);

            assertEquals(2, stats.size());
            assertEquals("Tiendas", stats.get(0).label());
            assertEquals(5L, stats.get(0).value());
            assertEquals("Presupuesto Total", stats.get(1).label());
            assertEquals(20000.0, stats.get(1).value());
        }

        @Test
        @DisplayName("Returns correct metrics for MANAGER role fetching local data")
        void getShopsStatistics_ForManager() {
            User manager = new User();
            manager.setId(88L);
            manager.setRoles(new java.util.HashSet<>(List.of("MANAGER")));

            when(shopRepository.countByAssignedManagerId(88L)).thenReturn(2L);
            when(shopRepository.sumAssignedBudgetsByManagerId(88L)).thenReturn(5000.0);

            List<StatDTO> stats = shopService.getShopsStatistics(manager);

            assertEquals(2, stats.size());
            assertEquals(2L, stats.get(0).value());
            assertEquals(5000.0, stats.get(1).value());
        }

        @Test
        @DisplayName("Returns empty metrics for users without relevant roles")
        void getShopsStatistics_ForUser() {
            User standardUser = new User();
            standardUser.setRoles(new java.util.HashSet<>(List.of("USER")));

            List<StatDTO> stats = shopService.getShopsStatistics(standardUser);

            assertTrue(stats.isEmpty());
        }
    }
}
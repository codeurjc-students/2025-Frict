package com.tfg.backend.integration;

import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.dto.ShopDTO;
import com.tfg.backend.model.*;
import com.tfg.backend.repository.*;
import com.tfg.backend.service.ImageService;
import com.tfg.backend.service.ShopService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ShopService.
 * Validates complex operations like safe shop deletion (cascading unlinks and order cancellations),
 * strict financial budget constraints during restocking, and repository metrics in MySQL.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ShopServiceITest {

    @Autowired private ShopService shopService;
    @Autowired private ShopRepository shopRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TruckRepository truckRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ShopStockRepository shopStockRepository;
    @Autowired private EntityManager entityManager;

    @MockitoBean private ImageService imageService;

    private User manager;
    private User customer;
    private Shop mainShop;
    private Shop secondaryShop;
    private Truck deliveryTruck;
    private Order pendingOrder;
    private Product expensiveProduct;
    private ShopStock shopStock;

    @BeforeEach
    void setUpComplexShopScenario() {
        // 1. Create and FORCE SAVE Manager
        manager = new User("Manager", "manager", "manager@test.com", "pass", "MANAGER");
        manager = userRepository.saveAndFlush(manager);

        // 2. Create and FORCE SAVE Customer
        customer = new User("Customer", "customer", "customer@test.com", "pass", "USER");
        customer = userRepository.saveAndFlush(customer);

        // 3. Create Shops
        mainShop = new Shop("Main Tech Shop", null, 1000.0); // Budget: 1000
        mainShop.setReferenceCode("SHOP-MAIN-123");
        mainShop.setAssignedManager(manager);
        mainShop = shopRepository.saveAndFlush(mainShop);

        secondaryShop = new Shop("Secondary Shop", null, 500.0);
        secondaryShop.setReferenceCode("SHOP-SEC-456");
        secondaryShop = shopRepository.saveAndFlush(secondaryShop);

        // 4. Assign Customer to Main Shop
        customer.setSelectedShop(mainShop);
        mainShop.getCustomers().add(customer);
        customer = userRepository.saveAndFlush(customer);
        mainShop = shopRepository.saveAndFlush(mainShop);

        // 5. Create Truck assigned to Main Shop
        deliveryTruck = new Truck();
        deliveryTruck.setReferenceCode("TRUCK-123");
        deliveryTruck.setPlateNumber("1234-ABC");
        deliveryTruck.setMaxOrderCapacity(10);
        deliveryTruck.setAssignedShop(mainShop);
        mainShop.getAssignedTrucks().add(deliveryTruck);
        deliveryTruck = truckRepository.saveAndFlush(deliveryTruck);

        // 6. Create Pending Order assigned to Main Shop
        pendingOrder = new Order();
        pendingOrder.setUser(customer);
        pendingOrder.setReferenceCode("ORD-123");
        pendingOrder.setAssignedShop(mainShop);
        pendingOrder.setTotalCost(100.0);
        pendingOrder.changeOrderStatus(OrderStatus.ORDER_MADE, "Order placed");
        mainShop.getAssignedOrders().add(pendingOrder);
        pendingOrder = orderRepository.saveAndFlush(pendingOrder);

        // 7. Create Product and Stock
        expensiveProduct = new Product("Server", "Enterprise Server", 2000.0, 800.0); // Supply price = 800
        expensiveProduct.setReferenceCode("PROD-SRV-1");
        expensiveProduct = productRepository.saveAndFlush(expensiveProduct);

        shopStock = new ShopStock(mainShop, expensiveProduct, 5); // Has 5 units initially
        shopStock = shopStockRepository.saveAndFlush(shopStock);

        // Clear cache to force clean reads from the database in tests
        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getUsername(), "pass", List.of(new SimpleGrantedAuthority("MANAGER")))
        );
    }

    @Test
    @DisplayName("Create Shop: Persists correctly with nested address")
    void testCreateShop_SavesAddressAndShop() {
        ShopDTO dto = new ShopDTO();
        dto.setName("New Outlet");
        dto.setAssignedBudget(2000.0);

        AddressDTO addressDTO = new AddressDTO();
        addressDTO.setStreet("Gran Via");
        addressDTO.setCity("Madrid");
        addressDTO.setCountry("Spain");
        dto.setAddress(addressDTO);

        Shop createdShop = shopService.createShop(dto);

        Shop dbShop = shopRepository.findById(createdShop.getId()).orElseThrow();
        assertAll(
                () -> assertEquals("New Outlet", dbShop.getName()),
                () -> assertEquals(2000.0, dbShop.getAssignedBudget()),
                () -> assertNotNull(dbShop.getAddress()),
                () -> assertEquals("Gran Via", dbShop.getAddress().getStreet())
        );
    }

    @Test
    @DisplayName("Delete Shop: Strictly nullifies relations and automatically cancels pending orders")
    void testDeleteShop_CascadesUnlinksAndCancelsOrders() {
        // Act: Delete the main shop
        shopService.deleteShop(mainShop.getId());

        // 1. Assert Shop is completely deleted
        assertFalse(shopRepository.existsById(mainShop.getId()), "Shop should be deleted from DB");

        // 2. Assert Truck is unlinked
        Truck dbTruck = truckRepository.findById(deliveryTruck.getId()).orElseThrow();
        assertNull(dbTruck.getAssignedShop(), "Truck must be unlinked from the deleted shop");

        // 3. Assert Customer is unlinked
        User dbCustomer = userRepository.findById(customer.getId()).orElseThrow();
        assertNull(dbCustomer.getSelectedShop(), "Customer's selected shop must be nullified");

        // 4. Assert Order is unlinked AND Cancelled
        Order dbOrder = orderRepository.findById(pendingOrder.getId()).orElseThrow();
        assertAll(
                () -> assertNull(dbOrder.getAssignedShop(), "Order must be unlinked from shop"),
                () -> assertEquals(OrderStatus.CANCELLED, dbOrder.getCurrentStatus(), "Pending order must be automatically cancelled"),
                () -> assertTrue(dbOrder.getHistory().getLast().getUpdates().getLast().getDescription().contains("eliminada"))
        );
    }

    @Test
    @DisplayName("Restock Product: Successfully increments stock units and reduces shop budget")
    void testRestockProduct_Success() {
        // Main shop budget: 1000.0. Product supply price: 800.0. Restocking 1 unit is allowed.
        shopService.restockProduct(shopStock.getId(), 1);

        Shop dbShop = shopRepository.findById(mainShop.getId()).orElseThrow();
        ShopStock dbStock = shopStockRepository.findById(shopStock.getId()).orElseThrow();

        assertAll(
                () -> assertEquals(6, dbStock.getUnits(), "Stock units should increment by 1 (5 + 1)"),
                () -> assertEquals(200.0, dbShop.getAssignedBudget(), "Shop budget should be reduced by supply cost (1000 - 800)")
        );
    }

    @Test
    @DisplayName("Restock Product: Throws FORBIDDEN if the shop lacks sufficient budget")
    void testRestockProduct_InsufficientBudget_ThrowsForbidden() {
        // Main shop budget: 1000.0. Product supply price: 800.0. Restocking 2 units costs 1600.0.
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> shopService.restockProduct(shopStock.getId(), 2));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertNotNull(ex.getReason());
        assertTrue(ex.getReason().contains("not enough budget"));
    }

    @Test
    @DisplayName("Metrics: Repository methods return accurate shop counts and budget sums based on roles")
    void testRepositoryMetrics() {
        // Configure security context as Manager to test the service routing
        authenticateUser(manager);

        // 1. Direct Repository checks for Admin queries
        long totalShops = shopRepository.count();
        double totalBudget = shopRepository.sumAllAssignedBudgets();

        // 2. Direct Repository checks for Manager queries
        long managerShops = shopRepository.countByAssignedManagerId(manager.getId());
        double managerBudget = shopRepository.sumAssignedBudgetsByManagerId(manager.getId());

        assertAll(
                () -> assertEquals(2, totalShops, "Admin should count all shops (2)"),
                () -> assertEquals(1500.0, totalBudget, "Admin should see sum of all budgets (1000 + 500)"),
                () -> assertEquals(1, managerShops, "Manager should only count their assigned shop (1)"),
                () -> assertEquals(1000.0, managerBudget, "Manager should only see their assigned shop's budget (1000)")
        );
    }
}
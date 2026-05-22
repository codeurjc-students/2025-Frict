package com.tfg.backend.integration;

import com.tfg.backend.model.*;
import com.tfg.backend.repository.*;
import com.tfg.backend.service.EmailService;
import com.tfg.backend.service.OrderService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

/**
 * Integration tests class for the OrderService.
 * Validates complex business rules including order creation, stock reduction,
 * cart clearing, status transition logic, and truck assignments directly against MySQL.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OrderServiceITest {

    @Autowired private OrderService orderService;

    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private ShopStockRepository shopStockRepository;
    @Autowired private TruckRepository truckRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private EntityManager entityManager;

    // Mock the email service to avoid sending real emails during testing
    @MockitoBean private EmailService emailService;

    private User buyer;
    private User driver;
    private Shop mainShop;
    private Product tvProduct;
    private Address shippingAddress;
    private PaymentCard paymentCard;
    private Truck deliveryTruck;
    private Order pendingOrder;

    @BeforeEach
    void setUpComplexScenario() {
        // 1. Create Shop and Truck
        mainShop = new Shop();
        mainShop.setName("Main Tech Shop");
        mainShop.setReferenceCode("SHOP-MAIN-123");
        mainShop.setAssignedBudget(10000.0);
        shopRepository.save(mainShop);

        driver = new User("Driver Test", "driver", "driver@test.com", "pass", "DRIVER");
        userRepository.save(driver);

        deliveryTruck = new Truck();
        deliveryTruck.setReferenceCode("TRUCK-DEL-123");
        deliveryTruck.setPlateNumber("1234-ABC");
        deliveryTruck.setAssignedShop(mainShop);
        deliveryTruck.setAssignedDriver(driver);
        deliveryTruck.setMaxCapacity(10);
        truckRepository.save(deliveryTruck);

        // 2. Create Buyer with Address, Card, and Selected Shop
        buyer = new User("Buyer Test", "buyer", "buyer@test.com", "pass", "USER");
        buyer.setSelectedShop(mainShop);

        shippingAddress = new Address("Home", "Fake St", "123", "A", "28000", "Madrid", "Spain");
        buyer.getAddresses().add(shippingAddress);

        paymentCard = new PaymentCard("Visa", "Buyer", "1111222233334444", "123", null);
        buyer.getCards().add(paymentCard);

        userRepository.save(buyer);

        // 3. Create Product with Stock in the Main Shop
        tvProduct = new Product("Smart TV", "4K Display", 1000.0, 800.0);
        ProductImageInfo img = new ProductImageInfo();
        img.setImageInfo(new ImageInfo("tv.jpg", "url", "tv.jpg"));
        img.setProduct(tvProduct);
        tvProduct.getImages().add(img);
        productRepository.save(tvProduct);

        ShopStock stock = new ShopStock(mainShop, tvProduct, 5); // 5 TVs available
        shopStockRepository.save(stock);

        // 4. Create an existing Order to test status changes
        pendingOrder = new Order(buyer, List.of(), mainShop, shippingAddress, paymentCard);
        pendingOrder.changeOrderStatus(OrderStatus.ORDER_MADE, "Order placed");
        orderRepository.save(pendingOrder);

        // Force DB sync
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Tests the massive logic inside createOrder:
     * Verifies cart retrieval, stock deduction from shop_stock, order persistence, and email triggering.
     */
    @Test
    @DisplayName("Create order correctly reduces stock, persists the order and triggers email")
    void testCreateOrder_Integration() {
        // 1. Setup: Authenticate buyer and put 2 TVs in their cart
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(buyer.getUsername(), "pass", java.util.List.of()));

        User activeBuyer = userRepository.findById(buyer.getId()).orElseThrow();
        OrderItem cartItem = new OrderItem(tvProduct, activeBuyer, 2);
        activeBuyer.getAllOrderItems().add(cartItem);
        userRepository.save(activeBuyer);

        // 2. Act: Call the real service to check out
        Order createdOrder = orderService.createOrder(shippingAddress.getId(), paymentCard.getId());

        // 3. Assert Order Creation
        assertNotNull(createdOrder.getId());
        assertEquals("4444", createdOrder.getCardNumberEnding(), "Should extract the last 4 digits of the card");
        assertEquals(OrderStatus.ORDER_MADE, createdOrder.getCurrentStatus());

        // 4. Assert Real Stock Reduction in DB
        Integer remainingStock = shopStockRepository.findUnitsByProductIdAndShopId(tvProduct.getId(), mainShop.getId()).orElse(0);
        assertEquals(3, remainingStock, "Stock should be 3 (5 original - 2 bought)");

        // 5. Assert cart item transformation (Product is unlinked, data is hardcoded)
        OrderItem savedItem = createdOrder.getItems().getFirst();
        assertNull(savedItem.getProduct(), "The product relationship must be unlinked to preserve history");
        assertEquals("Smart TV", savedItem.getProductName(), "The product name must be hardcoded");

        // 6. Assert Email was triggered
        verify(emailService).sendOrderConfirmation(anyString(), anyString(), anyString(), anyList(), anyDouble());
    }

    /**
     * Tests the strict logic that prevents setting an order to ON_DELIVERY without a truck.
     */
    @Test
    @DisplayName("Status update throws FORBIDDEN if changing to ON_DELIVERY without an assigned truck")
    void testUpdateStatus_OnDeliveryWithoutTruck_ThrowsForbidden() {
        // Pending order does not have a truck yet
        assertNull(pendingOrder.getAssignedTruck());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.commentAndOrUpdateOrderStatus(pendingOrder.getId(), OrderStatus.ON_DELIVERY, "Go"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertNotNull(ex.getReason());
        assertTrue(ex.getReason().contains("must have an associated delivery truck"));
    }

    /**
     * Tests that a driver can cancel an order, generating the correct status and history log.
     */
    @Test
    @DisplayName("Cancel order by driver generates the correct cancellation reason")
    void testCancelOrder_ByDriver() {
        // 1. Assign the truck to the order USING THE SERVICE.
        orderService.setAssignedTruck(pendingOrder.getId(), deliveryTruck.getId(), true);

        // 2. Authenticate as Driver
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(driver.getUsername(), "pass", java.util.List.of()));

        // 3. Act: The assigned driver cancels the order
        orderService.cancelOrder(pendingOrder.getId());

        // 4. Assert
        Order cancelledOrder = orderRepository.findById(pendingOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.CANCELLED, cancelledOrder.getCurrentStatus());

        OrderStatusLog lastLog = cancelledOrder.getHistory().getLast();
        assertEquals("El pedido ha sido cancelado por el repartidor.", lastLog.getUpdates().getLast().getDescription());
    }

    /**
     * Tests truck assignment validation: ensures the truck belongs to the same shop as the order.
     */
    @Test
    @DisplayName("Set assigned truck throws FORBIDDEN if the truck belongs to a different shop")
    void testSetAssignedTruck_DifferentShop_ThrowsForbidden() {
        // Create a rogue shop and truck
        Shop rogueShop = new Shop();
        rogueShop.setName("Rogue Shop");
        rogueShop.setReferenceCode("SHOP-ROGUE-999");
        rogueShop.setAssignedBudget(5000.0);
        shopRepository.save(rogueShop);

        Truck rogueTruck = new Truck();
        rogueTruck.setReferenceCode("TRUCK-ROG-999");
        rogueTruck.setPlateNumber("9999-ZZZ");
        rogueTruck.setAssignedShop(rogueShop);
        rogueTruck.setMaxCapacity(10);
        truckRepository.save(rogueTruck);

        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.setAssignedTruck(pendingOrder.getId(), rogueTruck.getId(), true));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertNotNull(ex.getReason());
        assertTrue(ex.getReason().contains("do not belong to the same shop"));
    }

    /**
     * Tests that placing an order is rejected when a cart product's stock is locally deactivated.
     */
    @Test
    @DisplayName("Create order throws METHOD_NOT_ALLOWED when a cart product has inactive local stock")
    void testCreateOrder_WithInactiveStock_ThrowsException() {
        // 1. Deactivate the TV stock in mainShop
        ShopStock stock = shopStockRepository.findByProduct_IdAndShop_Id(tvProduct.getId(), mainShop.getId()).orElseThrow();
        stock.setActive(false);
        shopStockRepository.save(stock);
        entityManager.flush();
        entityManager.clear();

        // 2. Authenticate buyer and put 1 TV in cart
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(buyer.getUsername(), "pass", java.util.List.of()));

        User activeBuyer = userRepository.findById(buyer.getId()).orElseThrow();
        OrderItem cartItem = new OrderItem(tvProduct, activeBuyer, 1);
        activeBuyer.getAllOrderItems().add(cartItem);
        userRepository.save(activeBuyer);

        // 3. Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.createOrder(shippingAddress.getId(), paymentCard.getId()));

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, ex.getStatusCode());
        assertTrue(ex.getReason().contains("no está disponible"));
    }

    /**
     * Tests the final deletion of an order, ensuring it's only possible if it's completed or cancelled.
     */
    @Test
    @DisplayName("Delete finished order removes it from DB only if status is COMPLETED or CANCELLED")
    void testDeleteFinishedOrder() {
        // 1. Attempt to delete pending order (should fail)
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orderService.deleteFinishedOrderById(pendingOrder.getId()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());

        // 2. Change status to COMPLETED
        Order orderToComplete = orderRepository.findById(pendingOrder.getId()).orElseThrow();
        orderToComplete.changeOrderStatus(OrderStatus.COMPLETED, "Done");
        orderRepository.save(orderToComplete);

        // 3. Attempt to delete again (should succeed)
        orderService.deleteFinishedOrderById(pendingOrder.getId());

        assertFalse(orderRepository.existsById(pendingOrder.getId()), "Order should be completely removed from DB");
    }
}
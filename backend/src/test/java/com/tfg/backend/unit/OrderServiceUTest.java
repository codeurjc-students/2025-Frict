package com.tfg.backend.unit;

import com.tfg.backend.dto.CartSummaryDTO;
import com.tfg.backend.model.*;
import com.tfg.backend.repository.OrderRepository;
import com.tfg.backend.service.*;
import com.tfg.backend.utils.SaveResult;
import com.tfg.backend.utils.StatDTO;
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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceUTest {

    @Mock private UserService userService;
    @Mock private ShopService shopService;
    @Mock private EmailService emailService;
    @Mock private OrderItemService orderItemService;
    @Mock private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private User loggedUser;
    private Shop selectedShop;
    private Address address;
    private PaymentCard card;

    @BeforeEach
    void setUp() {
        selectedShop = new Shop();
        selectedShop.setId(10L);
        selectedShop.setAssignedOrders(new ArrayList<>());

        address = new Address();
        address.setId(100L);

        card = new PaymentCard();
        card.setId(200L);
        card.setNumber("1234567890123456");

        loggedUser = new User();
        loggedUser.setId(1L);
        loggedUser.setEmail("user@test.com");
        loggedUser.setName("Test User");
        loggedUser.setSelectedShop(selectedShop);
        loggedUser.setAddresses(new ArrayList<>(List.of(address)));
        loggedUser.setCards(new ArrayList<>(List.of(card)));
        loggedUser.setRoles(new HashSet<>(List.of("USER")));
        loggedUser.setAllOrderItems(new ArrayList<>()); // Represents cart items
        loggedUser.setRegisteredOrders(new HashSet<>());
    }

    // --- ROLE-BASED ROUTING & METRICS ---
    @Nested
    @DisplayName("Role-based Read and Statistics Tests")
    class RoleBasedTests {
        @Test
        @DisplayName("getOrdersByRole routes to correct repository method based on roles")
        void getOrdersByRole_RoutesCorrectly() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> dummyPage = new PageImpl<>(List.of(new Order()));

            // Test Admin
            loggedUser.setRoles(Set.of("ADMIN"));
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(orderRepository.findAll(pageable)).thenReturn(dummyPage);
            assertEquals(dummyPage, orderService.getOrdersByRole(pageable));

            // Test Driver
            loggedUser.setRoles(Set.of("DRIVER"));
            when(orderRepository.findByAssignedTruck_AssignedDriver_Id(1L, pageable)).thenReturn(dummyPage);
            assertEquals(dummyPage, orderService.getOrdersByRole(pageable));

            // Test Manager (Fallback)
            loggedUser.setRoles(Set.of("MANAGER"));
            when(orderRepository.findByAssignedShop_AssignedManager_Id(1L, pageable)).thenReturn(dummyPage);
            assertEquals(dummyPage, orderService.getOrdersByRole(pageable));
        }

        @Test
        @DisplayName("getOrdersStatistics calculates totals properly for Admin using record accessors")
        void getOrdersStatistics_ForAdmin() {
            loggedUser.setRoles(Set.of("ADMIN"));
            when(orderRepository.countOrdersByStatusIn(List.of(OrderStatus.ORDER_MADE))).thenReturn(10L);
            when(orderRepository.countOrdersByStatusIn(List.of(OrderStatus.SENT))).thenReturn(5L);
            when(orderRepository.countOrdersByStatusIn(List.of(OrderStatus.ON_DELIVERY))).thenReturn(2L);
            when(orderRepository.countOrdersByStatusIn(List.of(OrderStatus.COMPLETED))).thenReturn(20L);

            List<StatDTO> stats = orderService.getOrdersStatistics(loggedUser);
            assertEquals(4, stats.size());
            assertEquals("Realizados", stats.get(0).label());
            assertEquals(10L, stats.get(0).value());
            assertEquals(20L, stats.get(3).value());
        }
    }

    // --- ORDER CREATION ---
    @Nested
    @DisplayName("Create Order Tests")
    class CreateOrderTests {
        @Test
        @DisplayName("Throws METHOD_NOT_ALLOWED if overall stock is less than cart quantity")
        void createOrder_ThrowsMethodNotAllowed_WhenStockIsInsufficient() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(shopService.findShopHelper(10L)).thenReturn(selectedShop);

            Product product = new Product();
            product.setName("Laptop");
            product.setShopsStock(List.of(new ShopStock(selectedShop, product, 2))); // 2 available

            OrderItem cartItem = new OrderItem();
            cartItem.setProduct(product);
            cartItem.setQuantity(5); // Requesting 5

            when(orderItemService.findUserCartItemsList(1L)).thenReturn(List.of(cartItem));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> orderService.createOrder(100L, 200L));
            assertEquals(HttpStatus.METHOD_NOT_ALLOWED, ex.getStatusCode());
        }

        @Test
        @DisplayName("Successfully distributes stock reduction across multiple shops and unlinks product")
        void createOrder_Success_ReducesDistributedStock() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(shopService.findShopHelper(10L)).thenReturn(selectedShop);

            Product product = new Product();
            product.setName("Phone");
            product.setCurrentPrice(500.0);

            // Nested image setup to avoid NullPointerException
            ProductImageInfo img = new ProductImageInfo();
            ImageInfo baseImage = new ImageInfo();
            baseImage.setImageUrl("url");
            img.setImageInfo(baseImage);
            product.setImages(List.of(img));

            // Stock split between two shops
            ShopStock stockShopA = new ShopStock(selectedShop, product, 2);
            ShopStock stockShopB = new ShopStock(new Shop(), product, 3);
            product.setShopsStock(List.of(stockShopA, stockShopB));

            OrderItem cartItem = new OrderItem();
            cartItem.setProduct(product);
            cartItem.setQuantity(4); // Takes 2 from A, 2 from B

            when(orderItemService.findUserCartItemsList(1L)).thenReturn(List.of(cartItem));

            Order newOrder = new Order();
            newOrder.setReferenceCode("REF-123");
            when(orderRepository.save(any(Order.class))).thenReturn(newOrder);

            orderService.createOrder(100L, 200L);

            // Verify stock reduction logic
            assertEquals(0, stockShopA.getUnits());
            assertEquals(1, stockShopB.getUnits());

            // Verify snapshotting
            assertNull(cartItem.getProduct());
            assertEquals("Phone", cartItem.getProductName());
            assertEquals(500.0, cartItem.getProductPrice());

            verify(emailService).sendOrderConfirmation(anyString(), anyString(), eq("REF-123"), anyList(), anyDouble());
        }
    }

    // --- STATUS UPDATES AND CANCELLATIONS ---
    @Nested
    @DisplayName("Status, Cancellations and Deletions Tests")
    class StatusAndCancellationTests {
        private Order order;

        @BeforeEach
        void setupOrder() {
            order = new Order();
            order.setId(1L);
            order.setUser(loggedUser);
        }

        @Test
        @DisplayName("commentAndOrUpdateOrderStatus adds comment inside existing status log if same status requested")
        void commentAndOrUpdateOrderStatus_AddsCommentOnly() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            orderService.commentAndOrUpdateOrderStatus(1L, OrderStatus.ORDER_MADE, "New comment");

            assertEquals(OrderStatus.ORDER_MADE, order.getCurrentStatus());
            assertEquals(1, order.getHistory().size()); // Still 1 status log
            assertEquals(2, order.getHistory().getLast().getUpdates().size()); // But 2 updates inside it
            assertEquals("New comment", order.getHistory().getLast().getUpdates().getLast().getDescription());
        }

        @Test
        @DisplayName("commentAndOrUpdateOrderStatus throws FORBIDDEN if transitioning to ON_DELIVERY without a truck")
        void commentAndOrUpdateOrderStatus_ThrowsForbidden_WhenNoTruck() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order)); // Order has no truck

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> orderService.commentAndOrUpdateOrderStatus(1L, OrderStatus.ON_DELIVERY, "Go"));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }

        @Test
        @DisplayName("cancelOrder throws FORBIDDEN if order does not belong to user")
        void cancelOrder_ThrowsForbidden_WhenNotUsersOrder() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(orderRepository.existsByIdAndUser(1L, loggedUser)).thenReturn(false);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> orderService.cancelOrder(1L));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }

        @Test
        @DisplayName("cancelOrder sets CANCELLED status with different reasons based on role")
        void cancelOrder_AppliesCorrectReason_BasedOnRole() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(orderRepository.existsByIdAndUser(1L, loggedUser)).thenReturn(true);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            // Test as USER
            orderService.cancelOrder(1L);
            assertEquals(OrderStatus.CANCELLED, order.getCurrentStatus());
            assertEquals("Has cancelado este pedido.", order.getHistory().getLast().getUpdates().getLast().getDescription());

            // Test as DRIVER
            loggedUser.setRoles(Set.of("DRIVER"));
            orderService.cancelOrder(1L);
            assertEquals("El pedido ha sido cancelado por el repartidor.", order.getHistory().getLast().getUpdates().getLast().getDescription());
        }

        @Test
        @DisplayName("deleteFinishedOrderById unlinks memory relations and deletes from DB")
        void deleteFinishedOrderById_Success() {
            order.changeOrderStatus(OrderStatus.COMPLETED, "Done");
            order.setAssignedShop(selectedShop);
            selectedShop.getAssignedOrders().add(order);
            loggedUser.getRegisteredOrders().add(order);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            orderService.deleteFinishedOrderById(1L);

            assertTrue(selectedShop.getAssignedOrders().isEmpty());
            assertTrue(loggedUser.getRegisteredOrders().isEmpty());
            verify(orderRepository).delete(order);
        }
    }

    // --- CART MANIPULATION ---
    @Nested
    @DisplayName("Cart Modification and Math Tests")
    class CartManipulationTests {
        private Product p1;
        private OrderItem cartItem;

        @BeforeEach
        void setupCart() {
            p1 = new Product();
            p1.setId(5L);
            p1.setCurrentPrice(10.0);
            p1.setPreviousPrice(15.0); // 5 discount
            p1.setShopsStock(List.of(new ShopStock(selectedShop, p1, 10))); // Max 10 achievable

            cartItem = new OrderItem();
            cartItem.setId(99L);
            cartItem.setProduct(p1);
            cartItem.setProductPrice(10.0);
            cartItem.setQuantity(2);
            // Must have order == null to be considered a cart item
            cartItem.setOrder(null);

            cartItem.setUser(loggedUser);

            loggedUser.getAllOrderItems().add(cartItem);
        }

        @Test
        @DisplayName("updateItemQuantity limits quantity to max achievable stock")
        void updateItemQuantity_LimitsToMaxStock() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

            // Requesting 50, but max stock is 10
            orderService.updateItemQuantity(5L, 50);

            assertEquals(10, cartItem.getQuantity(), "Quantity must be capped at max achievable stock");
        }

        @Test
        @DisplayName("updateItemQuantity sets quantity to 1 if requested is negative and stock is available")
        void updateItemQuantity_SetsToOne_WhenNegativeRequested() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

            orderService.updateItemQuantity(5L, -5);

            assertEquals(1, cartItem.getQuantity(), "Negative values default to 1 if stock exists");
        }

        @Test
        @DisplayName("deleteCartItem successfully removes item by ID")
        void deleteCartItem_Success() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

            orderService.deleteCartItem(99L);

            assertTrue(loggedUser.getAllOrderItems().isEmpty(), "Item should be removed from the list");
        }

        @Test
        @DisplayName("deleteCartItem throws NOT_FOUND if item does not exist")
        void deleteCartItem_ThrowsNotFound() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> orderService.deleteCartItem(1000L));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        @DisplayName("clearCartItems removes only items without an assigned order")
        void clearCartItems_RemovesOnlyCartItems() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

            // Add a purchased item (order != null)
            OrderItem purchasedItem = new OrderItem();
            purchasedItem.setOrder(new Order());
            purchasedItem.setUser(loggedUser);

            loggedUser.getAllOrderItems().add(purchasedItem);

            orderService.clearCartItems();

            assertEquals(1, loggedUser.getAllOrderItems().size(), "Purchased items must remain");
            assertTrue(loggedUser.getAllOrderItems().contains(purchasedItem), "Cart items must be cleared");
        }
    }
}
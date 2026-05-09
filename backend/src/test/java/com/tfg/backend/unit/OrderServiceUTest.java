package com.tfg.backend.unit;

import com.tfg.backend.dto.StatDTO;
import com.tfg.backend.model.*;
import com.tfg.backend.repository.OrderRepository;
import com.tfg.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
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

    @Mock
    private UserService userService;
    @Mock
    private ShopService shopService;
    @Mock
    private EmailService emailService;
    @Mock
    private OrderItemService orderItemService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher; //Necessary to avoid errors trying to send notifications, but not used

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
        selectedShop.setReferenceCode("SH-TEST");
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
        @DisplayName("Successfully reduces stock ONLY from selected shop, updates budget and unlinks product")
        void createOrder_Success_ReducesLocalStockAndUpdatesBudget() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

            // Set selected shop id and initial budget
            selectedShop.setId(10L);
            selectedShop.setAssignedBudget(1000.0);
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

            // Stock split between local shop and another shop
            Shop otherShop = new Shop();
            otherShop.setId(99L);

            // The local shop must have enough stock on its own
            ShopStock localStock = new ShopStock(selectedShop, product, 5);
            ShopStock remoteStock = new ShopStock(otherShop, product, 3);
            product.setShopsStock(List.of(localStock, remoteStock));

            OrderItem cartItem = new OrderItem();
            cartItem.setProduct(product);
            cartItem.setQuantity(4); // Order 4 units

            when(orderItemService.findUserCartItemsList(1L)).thenReturn(List.of(cartItem));

            Order savedOrderMock = new Order();
            savedOrderMock.setReferenceCode("REF-123");
            savedOrderMock.setTotalCost(2000.0);
            when(orderRepository.save(any(Order.class))).thenReturn(savedOrderMock);

            // ACT
            orderService.createOrder(100L, 200L);

            // ASSERTIONS
            // 1. Verify stock reduction logic (strictly local)
            assertEquals(1, localStock.getUnits()); // 5 - 4 = 1
            assertEquals(3, remoteStock.getUnits()); // Other shops should not modify their stock

            // 2. Verify budget update (Initial budget 1000 + order cost)
            assertTrue(selectedShop.getAssignedBudget() > 1000.0, "El presupuesto de la tienda debería haber aumentado tras la venta");

            // 3. Verify snapshotting
            assertNull(cartItem.getProduct());
            assertEquals("Phone", cartItem.getProductName());
            assertEquals(500.0, cartItem.getProductPrice());

            verify(emailService).sendOrderConfirmation(anyString(), anyString(), eq("REF-123"), anyList(), anyDouble());
        }


        @Test
        @DisplayName("Throws exception when local shop does not have enough stock")
        void createOrder_ThrowsException_WhenLocalStockInsufficient() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            selectedShop.setId(10L);
            when(shopService.findShopHelper(10L)).thenReturn(selectedShop);

            Product product = new Product();
            product.setName("Phone");

            Shop otherShop = new Shop();
            otherShop.setId(99L);

            // Selected shop has 1 unit, another shop has 10 units
            ShopStock localStock = new ShopStock(selectedShop, product, 1);
            ShopStock remoteStock = new ShopStock(otherShop, product, 10);
            product.setShopsStock(List.of(localStock, remoteStock));

            OrderItem cartItem = new OrderItem();
            cartItem.setProduct(product);
            cartItem.setQuantity(4); // Order 4 units, but selected shop only has 1 unit

            when(orderItemService.findUserCartItemsList(1L)).thenReturn(List.of(cartItem));

            // Check ResponseStatusException is thrown
            ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
                orderService.createOrder(100L, 200L);
            });

            assertEquals(HttpStatus.METHOD_NOT_ALLOWED, exception.getStatusCode());
            assertNotNull(exception.getReason());
            assertTrue(exception.getReason().contains("No hay suficiente stock"));
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
            order.setAssignedShop(selectedShop);
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

            User anotherUser = new User();
            anotherUser.setId(999L);
            order.setUser(anotherUser);
            order.setAssignedTruck(null);

            //Mock the user search
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> orderService.cancelOrder(1L));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }

        @Test
        @DisplayName("cancelOrder sets CANCELLED status with different reasons based on role")
        void cancelOrder_AppliesCorrectReason_BasedOnRole() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
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

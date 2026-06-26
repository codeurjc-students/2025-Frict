package com.tfg.backend.unit;

import com.tfg.backend.dto.CartSummaryDTO;
import com.tfg.backend.dto.StatDTO;
import com.tfg.backend.event.OrderEvent;
import com.tfg.backend.event.RegistryEvent;
import com.tfg.backend.event.ShopStockEvent;
import com.tfg.backend.model.*;
import com.tfg.backend.repository.OrderRepository;
import com.tfg.backend.repository.ShopStockRepository;
import com.tfg.backend.service.*;
import com.tfg.backend.utils.PdfService;
import com.tfg.backend.utils.SaveResult;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceUTest {

    @Mock private UserService userService;
    @Mock private ShopService shopService;
    @Mock private TruckService truckService;
    @Mock private EmailService emailService;
    @Mock private OrderItemService orderItemService;
    @Mock private ProductService productService;
    @Mock private PdfService pdfService;
    @Mock private OrderRepository orderRepository;
    @Mock private ShopStockRepository shopStockRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private User loggedUser;
    private Shop selectedShop;
    private Address address;
    private PaymentCard card;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "lowStockThreshold", 5);

        selectedShop = new Shop();
        selectedShop.setId(10L);
        selectedShop.setReferenceCode("SH-TEST");
        selectedShop.setName("Test Shop");
        selectedShop.setAssignedOrders(new ArrayList<>());

        address = new Address();
        address.setId(100L);

        card = new PaymentCard();
        card.setId(200L);
        card.setNumber("1234567890123456");

        loggedUser = new User();
        loggedUser.setId(1L);
        loggedUser.setName("Test User");
        loggedUser.setEmail("user@test.com");
        loggedUser.setSelectedShop(selectedShop);
        loggedUser.setAddresses(new ArrayList<>(List.of(address)));
        loggedUser.setCards(new ArrayList<>(List.of(card)));
        loggedUser.setRoles(new HashSet<>(List.of("USER")));
        loggedUser.setAllOrderItems(new ArrayList<>());
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
        @DisplayName("getOrdersStatistics calculates totals properly for Admin")
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

        @Test
        @DisplayName("getOrdersStatistics uses manager-scoped repository queries for MANAGER role")
        void getOrdersStatistics_ForManager() {
            loggedUser.setRoles(Set.of("MANAGER"));
            when(orderRepository.countOrdersByManagerIdAndStatusIn(1L, List.of(OrderStatus.ORDER_MADE))).thenReturn(3L);
            when(orderRepository.countOrdersByManagerIdAndStatusIn(1L, List.of(OrderStatus.SENT))).thenReturn(2L);
            when(orderRepository.countOrdersByManagerIdAndStatusIn(1L, List.of(OrderStatus.ON_DELIVERY))).thenReturn(1L);
            when(orderRepository.countOrdersByManagerIdAndStatusIn(1L, List.of(OrderStatus.COMPLETED))).thenReturn(8L);

            List<StatDTO> stats = orderService.getOrdersStatistics(loggedUser);
            assertEquals(4, stats.size());
            assertEquals(3L, stats.get(0).value());
            assertEquals(8L, stats.get(3).value());
        }

        @Test
        @DisplayName("getOrdersStatistics uses driver-scoped repository queries for DRIVER role")
        void getOrdersStatistics_ForDriver() {
            loggedUser.setRoles(Set.of("DRIVER"));
            when(orderRepository.countOrdersByDriverIdAndStatusIn(1L, List.of(OrderStatus.ORDER_MADE))).thenReturn(1L);
            when(orderRepository.countOrdersByDriverIdAndStatusIn(1L, List.of(OrderStatus.SENT))).thenReturn(4L);
            when(orderRepository.countOrdersByDriverIdAndStatusIn(1L, List.of(OrderStatus.ON_DELIVERY))).thenReturn(2L);
            when(orderRepository.countOrdersByDriverIdAndStatusIn(1L, List.of(OrderStatus.COMPLETED))).thenReturn(15L);

            List<StatDTO> stats = orderService.getOrdersStatistics(loggedUser);
            assertEquals(4, stats.size());
            assertEquals(1L, stats.get(0).value());
            assertEquals(15L, stats.get(3).value());
        }

        @Test
        @DisplayName("getUserOrdersByUserId delegates to correct repository after resolving user")
        void getUserOrdersByUserId_DelegatesToRepository() {
            User targetUser = new User();
            targetUser.setId(2L);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> expected = new PageImpl<>(List.of());

            when(userService.findUserHelper(2L)).thenReturn(targetUser);
            when(orderRepository.findAllByUser(targetUser, pageable)).thenReturn(expected);

            assertEquals(expected, orderService.getUserOrdersByUserId(2L, pageable));
        }

        @Test
        @DisplayName("getAllUserOrders returns orders scoped to the logged-in user")
        void getAllUserOrders_ReturnsLoggedUserOrders() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> expected = new PageImpl<>(List.of(new Order()));

            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(orderRepository.findAllByUser(loggedUser, pageable)).thenReturn(expected);

            assertEquals(expected, orderService.getAllUserOrders(pageable));
        }

        @Test
        @DisplayName("findOrderHelper throws NOT_FOUND when order does not exist")
        void findOrderHelper_ThrowsNotFound_WhenMissing() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> orderService.findOrderHelper(99L));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    // --- QR TOKEN ---
    @Nested
    @DisplayName("QR Token Tests")
    class QrTokenTests {
        private Order order;

        @BeforeEach
        void setupOrder() {
            loggedUser.setUsername("qr-owner");
            order = new Order();
            order.setId(5L);
            order.setUser(loggedUser);
            order.setAssignedShop(selectedShop);
            order.setReferenceCode("REF-QR");
        }

        @Test
        @DisplayName("getOrderQrToken returns the token when the requester is the order owner")
        void getOrderQrToken_ReturnsToken_WhenOwner() {
            String expectedToken = order.getQrDeliveryToken();
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

            assertEquals(expectedToken, orderService.getOrderQrToken(5L));
        }

        @Test
        @DisplayName("getOrderQrToken throws UNAUTHORIZED when requester is not the order owner")
        void getOrderQrToken_ThrowsUnauthorized_WhenNotOwner() {
            User otherUser = new User();
            otherUser.setUsername("someone-else");
            order.setUser(otherUser);

            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> orderService.getOrderQrToken(5L));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }

        @Test
        @DisplayName("checkOrderQrToken returns false when the provided token does not match")
        void checkOrderQrToken_ReturnsFalse_WhenTokenDoesNotMatch() {
            when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

            assertFalse(orderService.checkOrderQrToken(5L, "WRONG-TOKEN-XYZ"));
        }

        @Test
        @DisplayName("checkOrderQrToken returns true and transitions order to COMPLETED when token matches")
        void checkOrderQrToken_ReturnsTrue_AndCompletesOrder_WhenTokenMatches() {
            String validToken = order.getQrDeliveryToken();
            when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

            boolean result = orderService.checkOrderQrToken(5L, validToken);

            assertTrue(result);
            assertEquals(OrderStatus.COMPLETED, order.getCurrentStatus());
        }
    }

    // --- ORDER CREATION ---
    @Nested
    @DisplayName("Create Order Tests")
    class CreateOrderTests {

        @Test
        @DisplayName("createOrder throws FORBIDDEN when user has no shop selected")
        void createOrder_ThrowsForbidden_WhenNoShopSelected() {
            loggedUser.setSelectedShop(null);
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> orderService.createOrder(100L, 200L));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }

        @Test
        @DisplayName("createOrder throws NOT_FOUND when the address ID does not match any saved address")
        void createOrder_ThrowsNotFound_WhenAddressNotFound() {
            loggedUser.setAddresses(new ArrayList<>()); // No addresses
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(shopService.findShopHelper(10L)).thenReturn(selectedShop);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> orderService.createOrder(100L, 200L));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertTrue(ex.getReason().contains("Address"));
        }

        @Test
        @DisplayName("createOrder throws NOT_FOUND when the card ID does not match any saved card")
        void createOrder_ThrowsNotFound_WhenCardNotFound() {
            loggedUser.setCards(new ArrayList<>()); // No cards (address exists, card does not)
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(shopService.findShopHelper(10L)).thenReturn(selectedShop);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> orderService.createOrder(100L, 200L));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertTrue(ex.getReason().contains("Card"));
        }

        @Test
        @DisplayName("createOrder throws METHOD_NOT_ALLOWED if overall stock is less than cart quantity")
        void createOrder_ThrowsMethodNotAllowed_WhenStockIsInsufficient() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(shopService.findShopHelper(10L)).thenReturn(selectedShop);

            Product product = new Product();
            product.setName("Laptop");
            product.setShopsStock(List.of(new ShopStock(selectedShop, product, 2)));

            OrderItem cartItem = new OrderItem();
            cartItem.setProduct(product);
            cartItem.setQuantity(5);

            when(orderItemService.findUserCartItemsList(1L)).thenReturn(List.of(cartItem));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> orderService.createOrder(100L, 200L));
            assertEquals(HttpStatus.METHOD_NOT_ALLOWED, ex.getStatusCode());
        }

        @Test
        @DisplayName("createOrder reduces stock only from selected shop, updates budget and unlinks product")
        void createOrder_Success_ReducesLocalStockAndUpdatesBudget() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            selectedShop.setAssignedBudget(1000.0);
            when(shopService.findShopHelper(10L)).thenReturn(selectedShop);

            Product product = new Product();
            product.setName("Phone");
            product.setCurrentPrice(500.0);

            ProductImageInfo img = new ProductImageInfo();
            ImageInfo baseImage = new ImageInfo();
            baseImage.setImageUrl("url");
            img.setImageInfo(baseImage);
            product.setImages(List.of(img));

            Shop otherShop = new Shop();
            otherShop.setId(99L);

            ShopStock localStock = new ShopStock(selectedShop, product, 5);
            ShopStock remoteStock = new ShopStock(otherShop, product, 3);
            product.setShopsStock(List.of(localStock, remoteStock));

            OrderItem cartItem = new OrderItem();
            cartItem.setProduct(product);
            cartItem.setQuantity(4);

            when(orderItemService.findUserCartItemsList(1L)).thenReturn(List.of(cartItem));

            Order savedOrderMock = new Order();
            savedOrderMock.setReferenceCode("REF-123");
            savedOrderMock.setTotalCost(2000.0);
            when(orderRepository.save(any(Order.class))).thenReturn(savedOrderMock);

            orderService.createOrder(100L, 200L);

            assertEquals(1, localStock.getUnits());
            assertEquals(3, remoteStock.getUnits());
            assertTrue(selectedShop.getAssignedBudget() > 1000.0);
            assertNull(cartItem.getProduct());
            assertEquals("Phone", cartItem.getProductName());
            verify(emailService).sendOrderConfirmation(anyString(), any(Order.class));
        }

        @Test
        @DisplayName("createOrder throws exception when local shop does not have enough stock")
        void createOrder_ThrowsException_WhenLocalStockInsufficient() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            selectedShop.setId(10L);
            when(shopService.findShopHelper(10L)).thenReturn(selectedShop);

            Product product = new Product();
            product.setName("Phone");

            Shop otherShop = new Shop();
            otherShop.setId(99L);

            ShopStock localStock = new ShopStock(selectedShop, product, 1);
            ShopStock remoteStock = new ShopStock(otherShop, product, 10);
            product.setShopsStock(List.of(localStock, remoteStock));

            OrderItem cartItem = new OrderItem();
            cartItem.setProduct(product);
            cartItem.setQuantity(4);

            when(orderItemService.findUserCartItemsList(1L)).thenReturn(List.of(cartItem));

            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> orderService.createOrder(100L, 200L));
            assertEquals(HttpStatus.METHOD_NOT_ALLOWED, exception.getStatusCode());
            assertTrue(exception.getReason().contains("No hay suficiente stock"));
        }

        @Test
        @DisplayName("createOrder publishes ShopStockEvent.LOW_STOCK when stock crosses the threshold")
        void createOrder_PublishesLowStockEvent_WhenThresholdCrossed() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            selectedShop.setAssignedBudget(5000.0);
            when(shopService.findShopHelper(10L)).thenReturn(selectedShop);

            Product product = new Product();
            product.setId(1L);
            product.setName("Cola");
            product.setReferenceCode("PRD-001");
            product.setCurrentPrice(2.0);
            ProductImageInfo img = new ProductImageInfo();
            img.setImageInfo(new ImageInfo());
            product.setImages(List.of(img));

            // 7 units in stock, buying 4 → 3 remaining (crosses threshold of 5)
            ShopStock localStock = new ShopStock(selectedShop, product, 7);
            product.setShopsStock(List.of(localStock));

            OrderItem cartItem = new OrderItem();
            cartItem.setProduct(product);
            cartItem.setQuantity(4);

            when(orderItemService.findUserCartItemsList(1L)).thenReturn(List.of(cartItem));

            Order savedOrder = new Order();
            savedOrder.setReferenceCode("ORD-LOW");
            savedOrder.setTotalCost(8.0);
            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

            orderService.createOrder(100L, 200L);

            verify(eventPublisher).publishEvent(any(ShopStockEvent.class));
        }

        @Test
        @DisplayName("createOrder does not publish ShopStockEvent.LOW_STOCK when stock stays at or above threshold")
        void createOrder_DoesNotPublishLowStockEvent_WhenThresholdNotCrossed() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            selectedShop.setAssignedBudget(5000.0);
            when(shopService.findShopHelper(10L)).thenReturn(selectedShop);

            Product product = new Product();
            product.setId(2L);
            product.setName("Chips");
            product.setReferenceCode("PRD-002");
            product.setCurrentPrice(2.0);
            ProductImageInfo img = new ProductImageInfo();
            img.setImageInfo(new ImageInfo());
            product.setImages(List.of(img));

            // 10 units in stock, buying 3 → 7 remaining (still above threshold of 5)
            ShopStock localStock = new ShopStock(selectedShop, product, 10);
            product.setShopsStock(List.of(localStock));

            OrderItem cartItem = new OrderItem();
            cartItem.setProduct(product);
            cartItem.setQuantity(3);

            when(orderItemService.findUserCartItemsList(1L)).thenReturn(List.of(cartItem));

            Order savedOrder = new Order();
            savedOrder.setReferenceCode("ORD-OK");
            savedOrder.setTotalCost(6.0);
            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

            orderService.createOrder(100L, 200L);

            verify(eventPublisher, never()).publishEvent(any(ShopStockEvent.class));
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
            order.setReferenceCode("REF-STATUS");
        }

        @Test
        @DisplayName("commentAndOrUpdateOrderStatus adds comment inside existing status log if same status requested")
        void commentAndOrUpdateOrderStatus_AddsCommentOnly() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            orderService.commentAndOrUpdateOrderStatus(1L, OrderStatus.ORDER_MADE, "New comment");

            assertEquals(OrderStatus.ORDER_MADE, order.getCurrentStatus());
            assertEquals(1, order.getHistory().size());
            assertEquals(2, order.getHistory().getLast().getUpdates().size());
            assertEquals("New comment", order.getHistory().getLast().getUpdates().getLast().getDescription());
        }

        @Test
        @DisplayName("commentAndOrUpdateOrderStatus throws FORBIDDEN if trying to change status on an already cancelled order")
        void commentAndOrUpdateOrderStatus_ThrowsForbidden_WhenOrderAlreadyCancelled() {
            order.changeOrderStatus(OrderStatus.CANCELLED, "User cancelled");
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> orderService.commentAndOrUpdateOrderStatus(1L, OrderStatus.SENT, "Update"));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }

        @Test
        @DisplayName("commentAndOrUpdateOrderStatus throws FORBIDDEN if transitioning to ON_DELIVERY without a truck")
        void commentAndOrUpdateOrderStatus_ThrowsForbidden_WhenNoTruck() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> orderService.commentAndOrUpdateOrderStatus(1L, OrderStatus.ON_DELIVERY, "Go"));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }

        @Test
        @DisplayName("commentAndOrUpdateOrderStatus changes status and fires OrderEvent for non-terminal transitions")
        void commentAndOrUpdateOrderStatus_ChangesStatusSuccessfully() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            Order result = orderService.commentAndOrUpdateOrderStatus(1L, OrderStatus.SENT, "Shipped");

            assertEquals(OrderStatus.SENT, result.getCurrentStatus());
            verify(eventPublisher).publishEvent(any(OrderEvent.class));
        }

        @Test
        @DisplayName("commentAndOrUpdateOrderStatus publishes a driver RegistryEvent when COMPLETED with an assigned truck and driver")
        void commentAndOrUpdateOrderStatus_PublishesDriverRegistry_WhenCompletedWithTruck() {
            User driver = new User();
            driver.setUsername("driver");
            driver.setName("Driver Name");
            Truck truck = new Truck();
            truck.setAssignedDriver(driver);
            order.setAssignedTruck(truck);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

            orderService.commentAndOrUpdateOrderStatus(1L, OrderStatus.COMPLETED, "Delivered");

            assertEquals(OrderStatus.COMPLETED, order.getCurrentStatus());
            // 1 OrderEvent (STATUS_CHANGED) + 1 RegistryEvent (driver)
            verify(eventPublisher).publishEvent(any(OrderEvent.class));
            verify(eventPublisher).publishEvent(any(RegistryEvent.class));
        }

        @Test
        @DisplayName("commentAndOrUpdateOrderStatus to ON_DELIVERY releases shop capacity and publishes a SHOP_USED_CAPACITY registry")
        void commentAndOrUpdateOrderStatus_OnDelivery_ReleasesShopCapacity() {
            User driver = new User();
            driver.setUsername("driver");
            driver.setName("Driver Name");
            Truck truck = new Truck();
            truck.setAssignedDriver(driver);
            order.setAssignedTruck(truck);
            order.setTotalCapacity(4.0);
            selectedShop.setOccupiedCapacity(10.0);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            orderService.commentAndOrUpdateOrderStatus(1L, OrderStatus.ON_DELIVERY, "On the way");

            assertEquals(OrderStatus.ON_DELIVERY, order.getCurrentStatus());
            assertEquals(6.0, selectedShop.getOccupiedCapacity(), "Shop capacity must be released by the order capacity (10 - 4)");
            verify(eventPublisher).publishEvent(any(RegistryEvent.class)); // capacity registry
            verify(eventPublisher).publishEvent(any(OrderEvent.class));     // STATUS_CHANGED
        }

        @Test
        @DisplayName("commentAndOrUpdateOrderStatus to COMPLETED releases the assigned truck's capacity")
        void commentAndOrUpdateOrderStatus_Completed_ReleasesTruckCapacity() {
            User driver = new User();
            driver.setUsername("driver");
            driver.setName("Driver Name");
            Truck truck = new Truck();
            truck.setAssignedDriver(driver);
            truck.setCurrentCapacity(7.0);
            order.setAssignedTruck(truck);
            order.setTotalCapacity(3.0);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

            orderService.commentAndOrUpdateOrderStatus(1L, OrderStatus.COMPLETED, "Delivered");

            assertEquals(4.0, truck.getCurrentCapacity(), "Truck capacity must be reduced by the order capacity (7 - 3)");
        }

        @Test
        @DisplayName("commentAndOrUpdateOrderStatus to COMPLETED without an assigned truck publishes no driver RegistryEvent")
        void commentAndOrUpdateOrderStatus_CompletedWithoutTruck_NoDriverRegistry() {
            order.setAssignedTruck(null);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

            orderService.commentAndOrUpdateOrderStatus(1L, OrderStatus.COMPLETED, "Done");

            assertEquals(OrderStatus.COMPLETED, order.getCurrentStatus());
            verify(eventPublisher, never()).publishEvent(any(RegistryEvent.class));
            verify(eventPublisher).publishEvent(any(OrderEvent.class));
        }

        @Test
        @DisplayName("commentAndOrUpdateOrderStatus to CANCELLED restores stock, releases truck capacity and publishes capacity + driver registries")
        void commentAndOrUpdateOrderStatus_Cancelled_RestoresStockAndPublishesRegistries() {
            User driver = new User();
            driver.setUsername("driver");
            driver.setName("Driver Name");
            Truck truck = new Truck();
            truck.setAssignedDriver(driver);
            truck.setCurrentCapacity(5.0);
            order.setAssignedTruck(truck);
            order.setTotalCapacity(2.0);

            OrderItem item = new OrderItem();
            item.setProductReferenceCode("PRD-1");
            item.setQuantity(3);
            order.setItems(List.of(item));

            ShopStock stock = new ShopStock(selectedShop, new Product(), 4);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(shopStockRepository.findByShop_IdAndProduct_ReferenceCode(10L, "PRD-1")).thenReturn(Optional.of(stock));

            orderService.commentAndOrUpdateOrderStatus(1L, OrderStatus.CANCELLED, "Cancelled by admin");

            assertEquals(OrderStatus.CANCELLED, order.getCurrentStatus());
            assertEquals(7, stock.getUnits(), "Stock must be restored (4 + 3)");
            assertEquals(3.0, truck.getCurrentCapacity(), "Truck capacity must be released (5 - 2)");
            // capacity registry (CANCELLED) + driver registry (truck + driver)
            verify(eventPublisher, times(2)).publishEvent(any(RegistryEvent.class));
            verify(eventPublisher).publishEvent(any(OrderEvent.class));
        }

        @Test
        @DisplayName("cancelOrder restores shop stock for each item that has a product reference code")
        void cancelOrder_RestoresShopStock() {
            OrderItem item = new OrderItem();
            item.setProductReferenceCode("PRD-9");
            item.setQuantity(2);
            order.setItems(List.of(item));

            ShopStock stock = new ShopStock(selectedShop, new Product(), 1);

            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(shopStockRepository.findByShop_IdAndProduct_ReferenceCode(10L, "PRD-9")).thenReturn(Optional.of(stock));

            orderService.cancelOrder(1L);

            assertEquals(OrderStatus.CANCELLED, order.getCurrentStatus());
            assertEquals(3, stock.getUnits(), "Stock must be restored (1 + 2)");
        }

        @Test
        @DisplayName("cancelOrder restores shop capacity only when the order had reached ON_DELIVERY, and skips items with no reference code")
        void cancelOrder_RestoresShopCapacity_WhenWasOnDelivery_AndSkipsNullRefItems() {
            order.changeOrderStatus(OrderStatus.ON_DELIVERY, "On the way");
            order.setTotalCapacity(5.0);
            selectedShop.setOccupiedCapacity(8.0);

            OrderItem noRefItem = new OrderItem(); // null productReferenceCode → must be skipped
            noRefItem.setQuantity(2);
            order.setItems(List.of(noRefItem));

            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            orderService.cancelOrder(1L);

            assertEquals(OrderStatus.CANCELLED, order.getCurrentStatus());
            assertEquals(13.0, selectedShop.getOccupiedCapacity(), "Shop capacity must be restored (8 + 5) since order was ON_DELIVERY");
            verify(shopStockRepository, never()).findByShop_IdAndProduct_ReferenceCode(anyLong(), any());
        }

        @Test
        @DisplayName("cancelOrder throws FORBIDDEN if order does not belong to user")
        void cancelOrder_ThrowsForbidden_WhenNotUsersOrder() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

            User anotherUser = new User();
            anotherUser.setId(999L);
            order.setUser(anotherUser);
            order.setAssignedTruck(null);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> orderService.cancelOrder(1L));
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

        @Test
        @DisplayName("deleteFinishedOrderById also removes the order from the assigned truck delivery list")
        void deleteFinishedOrderById_AlsoUnlinksTruck_WhenAssigned() {
            order.changeOrderStatus(OrderStatus.COMPLETED, "Done");
            Truck truck = new Truck();
            truck.setOrdersToDeliver(new HashSet<>(List.of(order)));
            order.setAssignedTruck(truck);
            loggedUser.getRegisteredOrders().add(order);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            orderService.deleteFinishedOrderById(1L);

            assertTrue(truck.getOrdersToDeliver().isEmpty(), "Truck delivery list must be cleared when order is deleted");
            verify(orderRepository).delete(order);
        }
    }

    // --- TRUCK ASSIGNMENT ---
    @Nested
    @DisplayName("Truck Assignment and Unassignment Tests")
    class TruckAssignmentTests {
        private Order order;
        private Truck truck;

        @BeforeEach
        void setupScenario() {
            order = new Order();
            order.setId(1L);
            order.setAssignedShop(selectedShop);
            order.setReferenceCode("REF-TRUCK");

            truck = new Truck();
            truck.setId(5L);
            truck.setAssignedShop(selectedShop); // Same shop as order
            truck.setMaxCapacity(10);
            truck.setOrdersToDeliver(new HashSet<>());
        }

        @Test
        @DisplayName("setAssignedTruck returns same order without firing events when same truck is already assigned")
        void setAssignedTruck_ReturnsSameOrder_WhenSameTruckAlreadyAssigned() {
            order.setAssignedTruck(truck); // Already assigned
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(truckService.findTruckHelper(5L)).thenReturn(truck);

            Order result = orderService.setAssignedTruck(1L, 5L, true);

            assertSame(order, result);
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("setAssignedTruck throws BAD_REQUEST when adding the order would exceed truck capacity")
        void setAssignedTruck_ThrowsBadRequest_WhenTruckAtFullCapacity() {
            truck.setMaxCapacity(2);
            truck.setCurrentCapacity(2); // Already at max
            order.setTotalCapacity(1);   // Order needs 1 unit

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(truckService.findTruckHelper(5L)).thenReturn(truck);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> orderService.setAssignedTruck(1L, 5L, true));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        @DisplayName("setAssignedTruck assigns the truck and fires an OrderEvent on success")
        void setAssignedTruck_AssignsTruckSuccessfully() {
            User driver = new User();
            driver.setUsername("driver");
            truck.setAssignedDriver(driver);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(truckService.findTruckHelper(5L)).thenReturn(truck);

            Order result = orderService.setAssignedTruck(1L, 5L, true);

            assertEquals(truck, result.getAssignedTruck());
            verify(eventPublisher).publishEvent(any(OrderEvent.class));
        }

        @Test
        @DisplayName("setAssignedTruck unassigns the current truck and fires an OrderEvent when state is false")
        void setAssignedTruck_UnassignsTruck_WhenStateFalse() {
            User driver = new User();
            driver.setUsername("driver");
            truck.setAssignedDriver(driver);
            order.setAssignedTruck(truck);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            Order result = orderService.setAssignedTruck(1L, null, false);

            assertNull(result.getAssignedTruck());
            verify(eventPublisher).publishEvent(any(OrderEvent.class));
            verify(truckService, never()).findTruckHelper(any());
        }

        @Test
        @DisplayName("unassignAsFinished throws FORBIDDEN when order is not yet in a terminal status")
        void unassignAsFinished_ThrowsForbidden_WhenOrderNotFinished() {
            // Order is in ORDER_MADE status (not COMPLETED or CANCELLED)
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> orderService.unassignAsFinished(1L));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }

        @Test
        @DisplayName("unassignAsFinished throws NOT_FOUND when the order has no truck assigned")
        void unassignAsFinished_ThrowsNotFound_WhenNoTruckAssigned() {
            order.changeOrderStatus(OrderStatus.COMPLETED, "Done");
            order.setAssignedTruck(null);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> orderService.unassignAsFinished(1L));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        @DisplayName("unassignAsFinished throws FORBIDDEN when logged user is not the assigned driver")
        void unassignAsFinished_ThrowsForbidden_WhenNotAssignedDriver() {
            order.changeOrderStatus(OrderStatus.COMPLETED, "Done");
            User wrongDriver = new User();
            wrongDriver.setUsername("other-driver");
            truck.setAssignedDriver(wrongDriver);
            order.setAssignedTruck(truck);

            loggedUser.setUsername("logged-user"); // Different from "other-driver"
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> orderService.unassignAsFinished(1L));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }

        @Test
        @DisplayName("unassignAsFinished clears the assigned truck when the logged user is the driver")
        void unassignAsFinished_Success() {
            order.changeOrderStatus(OrderStatus.COMPLETED, "Done");
            loggedUser.setUsername("the-driver");
            truck.setAssignedDriver(loggedUser);
            order.setAssignedTruck(truck);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

            Order result = orderService.unassignAsFinished(1L);

            assertNull(result.getAssignedTruck());
        }

        @Test
        @DisplayName("unassignAsFinished throws NOT_FOUND when the assigned truck has no driver")
        void unassignAsFinished_ThrowsNotFound_WhenTruckHasNoDriver() {
            order.changeOrderStatus(OrderStatus.COMPLETED, "Done");
            truck.setAssignedDriver(null);
            order.setAssignedTruck(truck);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> orderService.unassignAsFinished(1L));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        @DisplayName("unassignAsFinished clears the truck's selected order when it points to this same order")
        void unassignAsFinished_ClearsSelectedOrder_WhenItIsThisOrder() {
            order.changeOrderStatus(OrderStatus.COMPLETED, "Done");
            loggedUser.setUsername("the-driver");
            truck.setAssignedDriver(loggedUser);
            truck.setSelectedOrder(order);
            order.setAssignedTruck(truck);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

            Order result = orderService.unassignAsFinished(1L);

            assertNull(result.getAssignedTruck());
            assertNull(truck.getSelectedOrder(), "Truck's selected order must be cleared when it is the unassigned order");
        }

        @Test
        @DisplayName("unassignAsFinished keeps the truck's selected order when it points to a different order")
        void unassignAsFinished_KeepsSelectedOrder_WhenDifferentOrder() {
            order.changeOrderStatus(OrderStatus.COMPLETED, "Done");
            loggedUser.setUsername("the-driver");
            truck.setAssignedDriver(loggedUser);

            Order otherSelected = new Order();
            otherSelected.setId(2L);
            truck.setSelectedOrder(otherSelected);
            order.setAssignedTruck(truck);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

            orderService.unassignAsFinished(1L);

            assertSame(otherSelected, truck.getSelectedOrder(), "A different selected order must remain untouched");
            assertNull(order.getAssignedTruck());
        }
    }

    // --- CART READ & SUMMARY ---
    @Nested
    @DisplayName("Cart Read and Summary Calculation Tests")
    class CartReadAndSummaryTests {

        @Test
        @DisplayName("getCartSummary returns all zeros with paid shipping when the cart is empty")
        void getCartSummary_EmptyCart_ReturnsZerosAndPaidShipping() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(orderItemService.findUserCartItemsList(1L)).thenReturn(List.of());

            CartSummaryDTO result = orderService.getCartSummary();

            assertAll(
                    () -> assertEquals(0, result.getTotalItems()),
                    () -> assertEquals(0.0, result.getTotalCost()),
                    () -> assertEquals(0.0, result.getTotalDiscount()),
                    () -> assertEquals(0.0, result.getShippingCost(), "Empty cart → no shipping cost")
            );
        }

        @Test
        @DisplayName("getCartSummary correctly calculates subtotal, discount and total when item has a previous price")
        void getCartSummary_WithDiscount_CalculatesCorrectly() {
            Product product = new Product();
            product.setCurrentPrice(10.0);
            product.setPreviousPrice(15.0); // 5 discount per unit

            OrderItem item = new OrderItem();
            item.setProductPrice(10.0);
            item.setProduct(product);
            item.setQuantity(3);

            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(orderItemService.findUserCartItemsList(1L)).thenReturn(List.of(item));

            CartSummaryDTO result = orderService.getCartSummary();

            assertAll(
                    () -> assertEquals(3, result.getTotalItems()),
                    () -> assertEquals(45.0, result.getSubtotalCost(), "Subtotal uses previous price: 15 * 3"),
                    () -> assertEquals(15.0, result.getTotalDiscount(), "(15-10) * 3"),
                    () -> assertEquals(30.0, result.getTotalCost(), "Total uses current price: 10 * 3"),
                    () -> assertEquals(5.0, result.getShippingCost(), "30 < 50 → paid shipping")
            );
        }

        @Test
        @DisplayName("getCartSummary applies free shipping when total exceeds 50")
        void getCartSummary_FreeShipping_WhenTotalExceeds50() {
            Product product = new Product();
            product.setCurrentPrice(20.0);
            product.setPreviousPrice(0.0); // No previous price

            OrderItem item = new OrderItem();
            item.setProductPrice(20.0);
            item.setProduct(product);
            item.setQuantity(3); // total = 60 > 50

            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(orderItemService.findUserCartItemsList(1L)).thenReturn(List.of(item));

            CartSummaryDTO result = orderService.getCartSummary();

            assertEquals(0.0, result.getShippingCost(), "60 > 50 → free shipping");
        }

        @Test
        @DisplayName("getCartItemByProductId returns the enriched item when found")
        void getCartItemByProductId_ReturnsItem_WhenFound() {
            Product product = new Product();
            product.setId(10L);
            OrderItem item = new OrderItem();
            item.setProduct(product);

            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(orderItemService.findUserCartItemByProductId(10L, 1L)).thenReturn(Optional.of(item));

            OrderItem result = orderService.getCartItemByProductId(10L);

            assertSame(item, result);
            verify(productService).enrichWithStock(product);
        }

        @Test
        @DisplayName("getCartItemByProductId returns null when the product is not in the cart")
        void getCartItemByProductId_ReturnsNull_WhenNotFound() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(orderItemService.findUserCartItemByProductId(99L, 1L)).thenReturn(Optional.empty());

            assertNull(orderService.getCartItemByProductId(99L));
            verify(productService, never()).enrichWithStock(any(Product.class));
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
            p1.setPreviousPrice(15.0);
            p1.setShopsStock(List.of(new ShopStock(selectedShop, p1, 10)));
            p1.setAvailableUnits(10);

            cartItem = new OrderItem();
            cartItem.setId(99L);
            cartItem.setProduct(p1);
            cartItem.setProductPrice(10.0);
            cartItem.setQuantity(2);
            cartItem.setOrder(null);
            cartItem.setUser(loggedUser);

            loggedUser.getAllOrderItems().add(cartItem);
        }

        @Test
        @DisplayName("addItemToCart increases quantity on existing cart item and returns isNew=false")
        void addItemToCart_UpdatesQuantity_WhenItemAlreadyInCart() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(productService.findProductHelper(5L)).thenReturn(p1);

            SaveResult<OrderItem> result = orderService.addItemToCart(5L, 3); // Add 3 more

            assertFalse(result.isNew());
            assertEquals(5, result.data().getQuantity(), "2 existing + 3 new = 5");
        }

        @Test
        @DisplayName("addItemToCart creates a new OrderItem and returns isNew=true when product not yet in cart")
        void addItemToCart_AddsNewItem_WhenNotInCart() {
            Product newProduct = new Product();
            newProduct.setId(20L);
            newProduct.setAvailableUnits(5);
            ProductImageInfo img = new ProductImageInfo();
            img.setImageInfo(new ImageInfo());
            newProduct.getImages().add(img);

            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(productService.findProductHelper(20L)).thenReturn(newProduct);

            SaveResult<OrderItem> result = orderService.addItemToCart(20L, 2);

            assertTrue(result.isNew());
            assertEquals(newProduct, result.data().getProduct());
            assertEquals(2, result.data().getQuantity());
            assertEquals(2, loggedUser.getAllOrderItems().size(), "New item must be added to the cart");
        }

        @Test
        @DisplayName("updateItemQuantity limits quantity to max achievable stock")
        void updateItemQuantity_LimitsToMaxStock() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

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
        @DisplayName("updateItemQuantity sets the exact requested quantity when it is within available stock")
        void updateItemQuantity_SetsNormalQuantity_WhenValid() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

            orderService.updateItemQuantity(5L, 7); // 7 <= 10 available

            assertEquals(7, cartItem.getQuantity());
        }

        @Test
        @DisplayName("updateItemQuantity throws NOT_FOUND when the product is not in the cart")
        void updateItemQuantity_ThrowsNotFound_WhenItemNotInCart() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> orderService.updateItemQuantity(999L, 3));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
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

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> orderService.deleteCartItem(1000L));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        @DisplayName("clearCartItems removes only items without an assigned order")
        void clearCartItems_RemovesOnlyCartItems() {
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);

            OrderItem purchasedItem = new OrderItem();
            purchasedItem.setOrder(new Order());
            purchasedItem.setUser(loggedUser);
            loggedUser.getAllOrderItems().add(purchasedItem);

            orderService.clearCartItems();

            assertEquals(1, loggedUser.getAllOrderItems().size(), "Purchased items must remain");
            assertTrue(loggedUser.getAllOrderItems().contains(purchasedItem));
        }
    }

    // --- PDF INVOICE ---
    @Nested
    @DisplayName("PDF Invoice Tests")
    class PdfTests {

        @Test
        @DisplayName("generateOrderPdfInvoice delegates to PdfService with the order and its QR token")
        void generateOrderPdfInvoice_DelegatesToPdfService() {
            loggedUser.setUsername("pdf-owner");
            Order order = new Order();
            order.setId(7L);
            order.setUser(loggedUser);
            String qrToken = order.getQrDeliveryToken();
            byte[] expectedPdf = new byte[]{1, 2, 3};

            when(orderRepository.findById(7L)).thenReturn(Optional.of(order));
            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(pdfService.generateOrderInvoicePdf(order, qrToken)).thenReturn(expectedPdf);

            byte[] result = orderService.generateOrderPdfInvoice(7L);

            assertArrayEquals(expectedPdf, result);
            verify(pdfService).generateOrderInvoicePdf(order, qrToken);
        }
    }
}

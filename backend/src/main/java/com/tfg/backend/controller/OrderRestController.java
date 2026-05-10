package com.tfg.backend.controller;

import com.tfg.backend.dto.*;
import com.tfg.backend.model.Order;
import com.tfg.backend.model.OrderItem;
import com.tfg.backend.model.OrderStatus;
import com.tfg.backend.service.OrderService;
import com.tfg.backend.service.ConnectionService;
import com.tfg.backend.utils.PageFormatter;
import com.tfg.backend.utils.SaveResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order Management", description = "Users orders data management")
@RequiredArgsConstructor
public class OrderRestController {

    private final OrderService orderService;

    // Inject the user connection service for presence enrichment
    private final ConnectionService connectionService;

    @Operation(summary = "(Admin, Manager, Driver) Get orders by role (paged)")
    @GetMapping("/")
    public ResponseEntity<PageResponse<OrderDTO>> getOrdersByRole(Pageable pageable){
        Page<Order> ordersByRole = orderService.getOrdersByRole(pageable);
        return ResponseEntity.ok(toEnrichedPageResponse(ordersByRole));
    }

    @Operation(summary = "(Admin) Get user orders by user ID (paged)")
    @GetMapping("/user/{id}")
    public ResponseEntity<PageResponse<OrderDTO>> getUserOrdersByUserId(@PathVariable Long id, Pageable pageable){
        Page<Order> userOrdersByUserId = orderService.getUserOrdersByUserId(id, pageable);
        return ResponseEntity.ok(toEnrichedPageResponse(userOrdersByUserId));
    }

    @Operation(summary = "(User) Get logged user orders (paged)")
    @GetMapping
    public ResponseEntity<PageResponse<OrderDTO>> getAllUserOrders(Pageable pageable){
        Page<Order> allUserOrders = orderService.getAllUserOrders(pageable);
        return ResponseEntity.ok(toEnrichedPageResponse(allUserOrders));
    }

    @Operation(summary = "(User) Get order by ID")
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id){
        Order order = orderService.findOrderHelper(id);
        return ResponseEntity.ok(toEnrichedDTO(order));
    }


    @Operation(summary = "(User) Get order QR token by ID")
    @GetMapping("/{id}/token")
    public ResponseEntity<String> getOrderQrTokenById(@PathVariable Long id){
        String token = orderService.getOrderQrToken(id);
        return ResponseEntity.ok(token);
    }


    @Operation(summary = "(Driver) Check order QR token by ID")
    @PostMapping("/{id}/token")
    public ResponseEntity<Boolean> checkOrderQrTokenById(@PathVariable Long id, @RequestBody Map<String, String> payload){
        String cleanToken = payload.get("token");
        boolean status = orderService.checkOrderQrToken(id, cleanToken);
        return ResponseEntity.ok(status);
    }


    @Operation(summary = "(Admin, Manager) Set assigned truck to an order")
    @PostMapping("/{orderId}/assign/truck/{truckId}")
    public ResponseEntity<OrderDTO> setAssignedTruck(
            @PathVariable Long orderId,
            @PathVariable Long truckId,
            @RequestParam boolean state) {
        Order savedOrder = orderService.setAssignedTruck(orderId, truckId, state);
        return ResponseEntity.ok(toEnrichedDTO(savedOrder));
    }


    @Operation(summary = "(Driver) Unassign completed or cancelled order")
    @PostMapping("/{orderId}/unassign")
    public ResponseEntity<OrderDTO> unassignAsFinished(@PathVariable Long orderId) {
        Order savedOrder = orderService.unassignAsFinished(orderId);
        return ResponseEntity.ok(toEnrichedDTO(savedOrder));
    }

    // Option 1 (active): CartSummaryDTO does not include the cart items list, finishing orders will require 2 queries to DB
    // Option 2: CartSummaryDTO includes the cart items list, and it is called from createdOrder to complete the order in 1 query (sends unnecessary information to frontend)
    @Operation(summary = "(User) Create order for logged user")
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestParam Long addressId,
                                                @RequestParam Long cardId){
        Order savedOrder = orderService.createOrder(addressId, cardId);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedOrder.getId())
                .toUri();

        return ResponseEntity.created(location).body(toEnrichedDTO(savedOrder));
    }

    @Operation(summary = "(Admin, Manager, Driver) Comment and/or update order status by ID")
    @PutMapping("/{id}")
    public ResponseEntity<OrderDTO> commentAndOrUpdateOrderStatus(@PathVariable Long id,
                                                                  @RequestParam OrderStatus orderStatus,
                                                                  @RequestParam(required = false) String comment){
        Order savedOrder = orderService.commentAndOrUpdateOrderStatus(id, orderStatus, comment);
        return ResponseEntity.ok(toEnrichedDTO(savedOrder));
    }

    @Operation(summary = "(User) Cancel logged user order by ID")
    @PutMapping("/cancel/{id}")
    public ResponseEntity<OrderDTO> cancelOrder(@PathVariable Long id){
        Order savedOrder = orderService.cancelOrder(id);
        return ResponseEntity.ok(toEnrichedDTO(savedOrder));
    }

    @Transactional
    @Operation(summary = "(Admin) Delete cancelled or completed order by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<OrderDTO> deleteFinishedOrderById(@PathVariable Long id){
        Order deletedOrder = orderService.deleteFinishedOrderById(id);
        return ResponseEntity.ok(toEnrichedDTO(deletedOrder));
    }

    @Operation(summary = "(User) Get logged user cart summary")
    @GetMapping("/cart/summary")
    public ResponseEntity<CartSummaryDTO> getCartSummary() {
        return ResponseEntity.ok(orderService.getCartSummary());
    }

    // Cart items of a user: items which order_id in DB is null and user_id is the same as the logged user id
    @Operation(summary = "(User) Get logged user cart products (paged)")
    @GetMapping("/cart")
    public ResponseEntity<PageResponse<OrderItemDTO>> getCartItemsPage(Pageable pageable) {
        Page<OrderItem> cartItems = orderService.getCartItemsPage(pageable);
        return ResponseEntity.ok(PageFormatter.toPageResponse(cartItems, OrderItemDTO::new));
    }

    @Operation(summary = "(User) Get logged user cart item by product ID")
    @GetMapping("/cart/item/{id}")
    public ResponseEntity<OrderItemDTO> getCartItemByProductId(@PathVariable Long id) {
        OrderItem item = orderService.getCartItemByProductId(id);
        if (item == null){
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(new OrderItemDTO(item));
    }

    @Operation(summary = "(User) Clear logged user cart products")
    @DeleteMapping("/cart")
    public ResponseEntity<CartSummaryDTO> clearCartItems() {
        return ResponseEntity.ok(orderService.clearCartItems());
    }

    @Operation(summary = "(User) Add item to logged user cart")
    @PostMapping("/cart/{id}")
    public ResponseEntity<OrderItemDTO> addItemToCart(@PathVariable Long id,
                                                      @RequestParam int quantity) {
        SaveResult<OrderItem> result = orderService.addItemToCart(id, quantity);
        OrderItem resultItem = result.data();

        if (result.isNew()) {
            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(resultItem.getId())
                    .toUri();

            return ResponseEntity.created(location).body(new OrderItemDTO(resultItem));
        } else {
            return ResponseEntity.ok(new OrderItemDTO(resultItem));
        }
    }

    @Operation(summary = "(User) Update logged user cart product quantity")
    @PutMapping("/cart/{id}")
    public ResponseEntity<CartSummaryDTO> updateItemQuantity(@PathVariable Long id,
                                                             @RequestParam int quantity) {
        orderService.updateItemQuantity(id, quantity);
        return ResponseEntity.ok(orderService.getCartSummary());
    }

    @Operation(summary = "(User) Delete logged user cart item")
    @DeleteMapping("/cart/{id}")
    public ResponseEntity<CartSummaryDTO> deleteCartItem(@PathVariable Long id) {
        orderService.deleteCartItem(id);
        return this.getCartSummary();
    }

    @Operation(summary = "(User) Download order PDF invoice")
    @GetMapping("/{id}/invoice")
    public ResponseEntity<byte[]> downloadOrderInvoice(@PathVariable("id") Long orderId) {
        byte[] pdfBytes = orderService.generateOrderPdfInvoice(orderId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);

        String filename = "Factura.pdf";
        headers.setContentDispositionFormData("attachment", filename);

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    // ==========================================
    // ASSEMBLER AND ENRICHMENT HELPER METHODS
    // ==========================================

    /**
     * Converts an Order entity to a DTO and enriches its user.
     */
    private OrderDTO toEnrichedDTO(Order order) {
        OrderDTO dto = new OrderDTO(order);
        if (dto.getUser() != null) {
            connectionService.enrichWithConnection(dto.getUser());
        }
        return dto;
    }

    /**
     * Converts a page of Order entities to a paginated response of enriched DTOs.
     */
    private PageResponse<OrderDTO> toEnrichedPageResponse(Page<Order> orders) {
        // 1. Map the pure entity page to a DTO page
        Page<OrderDTO> dtoPage = orders.map(OrderDTO::new);

        // 2. Extract users from the current page and enrich them in a single batch query
        List<UserDTO> users = dtoPage.getContent().stream()
                .map(OrderDTO::getUser)
                .filter(Objects::nonNull)
                .toList();

        connectionService.enrichWithConnections(users);

        // 3. Return the formatted PageResponse
        return PageFormatter.toPageResponse(dtoPage, dto -> dto);
    }
}
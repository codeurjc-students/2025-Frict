package com.tfg.backend.controller;

import com.tfg.backend.dto.CartSummaryDTO;
import com.tfg.backend.dto.OrderDTO;
import com.tfg.backend.dto.OrderItemDTO;
import com.tfg.backend.dto.PageResponse;
import com.tfg.backend.model.*;
import com.tfg.backend.service.OrderItemService;
import com.tfg.backend.service.OrderService;
import com.tfg.backend.service.ProductService;
import com.tfg.backend.service.UserService;
import com.tfg.backend.utils.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order Management", description = "Users orders data management")
public class OrderRestController {

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private EmailService emailService;


    @Operation(summary = "Get logged user orders (paged)")
    @GetMapping
    public ResponseEntity<PageResponse<OrderDTO>> getAllUserOrders(HttpServletRequest request, Pageable pageable){
        //Get logged user info if any (User class)
        User loggedUser = findLoggedUserHelper(request);

        Page<Order> userOrders = orderService.findAllByUser(loggedUser, pageable);
        return ResponseEntity.ok(toPageResponse(userOrders));
    }


    @Operation(summary = "Get order by ID")
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id){
        Optional<Order> orderOptional = this.orderService.findById(id);

        if(orderOptional.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order with ID " + id + " does not exist.");
        }
        return ResponseEntity.ok(new OrderDTO(orderOptional.get()));
    }


    //Option 1 (active): CartSummaryDTO does not include the cart items list, finishing orders will require 2 queries to DB
    //Option 2: CartSummaryDTO includes the cart items list, and it is called from createdOrder to complete the order in 1 query (sends unnecessary information to frontend)
    @Operation(summary = "Create order for logged user")
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(HttpServletRequest request,
                                                @RequestParam Long addressId,
                                                @RequestParam Long cardId){

        //Get logged user info if any (User class)
        User loggedUser = findLoggedUserHelper(request);

        //Find address info and card info
        Optional<Address> addressOptional = loggedUser.getAddresses().stream()
                .filter(addr -> addr.getId().equals(addressId))
                .findFirst();

        Optional<PaymentCard> cardOptional = loggedUser.getCards().stream()
                .filter(card -> card.getId().equals(cardId))
                .findFirst();

        if (addressOptional.isEmpty() || cardOptional.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Address with ID " + addressId + " or card with ID " + cardId + " not found.");
        }

        //Find cart items
        List<OrderItem> cartItems = orderItemService.findUserCartItemsList(loggedUser.getId());

        //Set the snapshot fields for later order details queries
        for (OrderItem i : cartItems) {
            i.setProductName(i.getProduct().getName());
            i.setProductImageUrl(i.getProduct().getImages().getFirst().getImageUrl());
            i.setProductPrice(i.getProduct().getCurrentPrice());
            i.setProduct(null); //This order item no longer depends on the current product
        }
        List<OrderItem> savedItems = orderItemService.saveAll(cartItems);

        Order newOrder = new Order(loggedUser, savedItems, addressOptional.get(), cardOptional.get());

        newOrder.setFullSendingAddress(addressOptional.get().toString());
        PaymentCard card = cardOptional.get();
        newOrder.setCardNumberEnding(card.getNumber().substring(card.getNumber().length() - 4));
        Order savedOrder = orderService.save(newOrder);

        //Send email confirmation
        emailService.sendOrderConfirmation(
                loggedUser.getEmail(),
                loggedUser.getName(),
                savedOrder.getReferenceCode(),
                savedOrder.getItems(),
                savedOrder.getTotalCost()
        );


        return ResponseEntity.ok(new OrderDTO(savedOrder));
    }


    @Operation(summary = "Cancel logged user order by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<OrderDTO> cancelOrder(HttpServletRequest request, @PathVariable Long id){
        //Get logged user info if any (User class)
        User loggedUser = findLoggedUserHelper(request);

        //Check if the order belongs to that user or if the logged user is a delivery driver
        if(!loggedUser.getRoles().contains("DRIVER") && !orderService.existsByIdAndUser(id, loggedUser)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order with ID " + id + " does not belong to this user or logged user has not delivery driver permissions.");
        }

        //Check if the order exists
        Optional<Order> orderOptional = orderService.findById(id);
        if (orderOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order with ID " + id + " does not exist.");
        }
        Order order = orderOptional.get();

        //Mark the order as cancelled (update order status without deleting it from DB)
        if(loggedUser.getRoles().contains("DRIVER")){
            order.changeOrderStatus(OrderStatus.CANCELLED, "El pedido ha sido cancelado por el repartidor.");
        }
        else{
            order.changeOrderStatus(OrderStatus.CANCELLED, "Has cancelado este pedido.");
        }

        Order savedOrder = orderService.save(order);
        return ResponseEntity.ok(new OrderDTO(savedOrder));
    }


    @Operation(summary = "Get logged user cart summary")
    @GetMapping("/cart/summary")
    public ResponseEntity<CartSummaryDTO> getCartSummary(HttpServletRequest request) {
        //Get logged user info if any (User class)
        User loggedUser = findLoggedUserHelper(request);

        List<OrderItem> cartItems = orderItemService.findUserCartItemsList(loggedUser.getId());

        int totalItems = 0;
        double subtotal = 0.0;
        double totalDiscount = 0.0;
        double total = 0.0;

        for (OrderItem item : cartItems) {
            Product p = item.getProduct();
            double currentPrice = item.getProductPrice();
            double previousPrice = 0.0;
            if(p != null){
                previousPrice = p.getPreviousPrice();
            }

            int quantity = item.getQuantity();
            totalItems += quantity;

            // Subtotal
            double unitSubtotal = (previousPrice > 0) ? previousPrice : currentPrice;
            subtotal += unitSubtotal * quantity;

            // Total
            total += currentPrice * quantity;

            // Discount
            if (previousPrice > currentPrice) {
                totalDiscount += (previousPrice - currentPrice) * quantity;
            }
        }

        double shippingCost = (total > 50.0) ? 0.0 : 5.0;

        //Rounded to 2 decimals, as it is currency
        return ResponseEntity.ok(new CartSummaryDTO(totalItems,
                Math.round(subtotal * 100.0) / 100.0,
                Math.round(totalDiscount * 100.0) / 100.0,
                shippingCost,
                Math.round(total * 100.0) / 100.0));
    }


    //Cart items of a user: items which order_id in DB is null and user_id is the same as the logged user id
    @Operation(summary = "Get logged user cart products (paged)")
    @GetMapping("/cart")
    public ResponseEntity<PageResponse<OrderItemDTO>> getCartItemsPage(HttpServletRequest request, Pageable pageable) {
        //Get logged user info if any (User class)
        User loggedUser = findLoggedUserHelper(request);

        Page<OrderItem> cartItems = orderItemService.findUserCartItemsPage(loggedUser.getId(), pageable);
        return ResponseEntity.ok(toOrderItemsPageDTO(cartItems));
    }


    @Operation(summary = "Clear logged user cart products")
    @DeleteMapping("/cart")
    public ResponseEntity<CartSummaryDTO> clearCartItems(HttpServletRequest request) {
        //Get logged user info if any (User class)
        User loggedUser = findLoggedUserHelper(request);

        List<OrderItem> itemsToRemove = loggedUser.getItemsInCart();

        if (!itemsToRemove.isEmpty()) {
            loggedUser.getAllOrderItems().removeAll(itemsToRemove);
            userService.save(loggedUser);
        }
        return this.getCartSummary(request);
    }


    @Operation(summary = "Add item to logged user cart")
    @PostMapping("/cart/{id}")
    public ResponseEntity<OrderItemDTO> addItemToCart(HttpServletRequest request,
                                                      @PathVariable Long id,
                                                      @RequestParam int quantity) {
        //Get logged user info if any (User class)
        User loggedUser = findLoggedUserHelper(request);

        //Check the product exists
        Optional<Product> productOptional = productService.findById(id);
        if(productOptional.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product with ID " + id + " does not exist.");
        }
        Product product = productOptional.get();

        //Check there is stock left to complete the operation (in cart + quantity <= stock?)
        List<OrderItem> inCartItems = orderItemService.findProductUnitsInCart(id);
        int inCartUnits = inCartItems.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        int inStockUnits = 0;
        for (ShopStock s : product.getShopsStock()) {
            inStockUnits += s.getStock();
        }

        if(inCartUnits + quantity > inStockUnits){
            //Code linked in frontend
            throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Stock of product with ID" + id + " is not enough to perform this operation.");
        }

        //Check if the product is in user's cart, and if so, update item quantity
        Optional<OrderItem> itemInCart = loggedUser.getItemsInCart().stream()
                .filter(item -> item.getProduct().getId().equals(id))
                .findFirst();

        OrderItem resultItem;

        if (itemInCart.isPresent()) {
            // Item in cart -> Update quantity
            resultItem = itemInCart.get();
            resultItem.setQuantity(resultItem.getQuantity() + quantity);
        } else {
            // Item not found -> Create item
            resultItem = new OrderItem(productOptional.get(), loggedUser, quantity);
            loggedUser.getAllOrderItems().add(resultItem);
        }

        userService.save(loggedUser);
        return ResponseEntity.ok(new OrderItemDTO(resultItem)); //Returns the added item (optional)
    }


    @Operation(summary = "Update logged user cart product quantity")
    @PutMapping("/cart/{id}")
    public ResponseEntity<CartSummaryDTO> updateItemQuantity(HttpServletRequest request,
                                                           @PathVariable Long id,
                                                           @RequestParam int quantity) {
        //Get logged user info if any (User class)
        User loggedUser = findLoggedUserHelper(request);

        // 2. Get the cart item (needed to know how many items of this product does the user already have)
        Optional<OrderItem> itemInCartOptional = loggedUser.getItemsInCart().stream()
                .filter(item -> item.getProduct().getId().equals(id))
                .findFirst();

        if (itemInCartOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item in cart with ID " + id + " does not exist.");
        }
        OrderItem itemToUpdate = itemInCartOptional.get();

        // 3. Get product (to check total stock)
        Product product = itemToUpdate.getProduct();
        int maxAchievableQuantity = product.getShopsStock().stream().mapToInt(ShopStock::getStock).sum();

        // CASE A: Quantity is bigger than available
        if (quantity > maxAchievableQuantity) {
            quantity = maxAchievableQuantity; // Maximum available units
        }
        else if (quantity < 0) {
            if(maxAchievableQuantity > 0){
                quantity = 1;
            }
            else {
                throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Updating order item not available.");
            }
        }

        // 6. Directly update quantity
        itemToUpdate.setQuantity(quantity);
        userService.save(loggedUser);
        return this.getCartSummary(request);
    }


    @Operation(summary = "Delete logged user cart item")
    @DeleteMapping("/cart/{id}")
    public ResponseEntity<CartSummaryDTO> deleteCartItem(HttpServletRequest request, @PathVariable Long id) {
        //Get logged user info if any (User class)
        User loggedUser = findLoggedUserHelper(request);

        List<OrderItem> orderItems = loggedUser.getAllOrderItems();
        boolean removed = orderItems.removeIf(i ->
            i.getOrder() == null &&
            i.getProduct() != null &&
            i.getProduct().getId() != null &&
            i.getId().equals(id)
        );

        if(!removed){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item with ID " + id + " not deleted as it does not exist.");
        }
        else {
            userService.save(loggedUser);
        }
        return this.getCartSummary(request);
    }


    private User findLoggedUserHelper(HttpServletRequest request) {
        return this.userService.getLoggedUser(request)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You must be logged to perform this operation."));
    }


    //Creates OrderPageDTO objects with necessary fields only
    private PageResponse<OrderDTO> toPageResponse(Page<Order> orders){
        List<OrderDTO> dtos = new ArrayList<>();
        for (Order o : orders.getContent()) {
            OrderDTO dto = new OrderDTO(o);
            dtos.add(dto);
        }
        return new PageResponse<>(dtos, orders.getTotalElements(), orders.getNumber(), orders.getTotalPages()-1, orders.getSize());
    }


    //Creates OrderItemsPageDTO objects with necessary fields only
    private PageResponse<OrderItemDTO> toOrderItemsPageDTO(Page<OrderItem> items){
        List<OrderItemDTO> dtos = new ArrayList<>();
        for (OrderItem i : items.getContent()) {
            OrderItemDTO dto = new OrderItemDTO(i);
            dtos.add(dto);
        }
        return new PageResponse<>(dtos, items.getTotalElements(), items.getNumber(), items.getTotalPages()-1, items.getSize());
    }
}

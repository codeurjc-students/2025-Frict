package com.tfg.backend.controller;

import com.tfg.backend.DTO.*;
import com.tfg.backend.model.*;
import com.tfg.backend.service.OrderItemService;
import com.tfg.backend.service.OrderService;
import com.tfg.backend.service.ProductService;
import com.tfg.backend.service.UserService;
import com.tfg.backend.utils.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/orders")
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

    //Option 1 (active): CartSummaryDTO does not include the cart items list, finishing orders will require 2 queries to DB
    //Option 2: CartSummaryDTO includes the cart items list, and it is called from createdOrder to complete the order in 1 query (sends unnecessary information to frontend)
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(HttpServletRequest request,
                                                @RequestParam Long addressId,
                                                @RequestParam Long cardId){

        //Get logged user info if any (User class)
        Optional<User> userOptional = userService.getLoggedUser(request);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        //Find cart items
        List<OrderItem> cartItems = orderItemService.findUserCartItemsList(loggedUser.getId());
        Order newOrder = new Order(loggedUser, cartItems);

        //Find address info and card info
        Optional<Address> addressOptional = loggedUser.getAddresses().stream()
                .filter(addr -> addr.getId().equals(addressId))
                .findFirst();

        Optional<PaymentCard> cardOptional = loggedUser.getCards().stream()
                .filter(card -> card.getId().equals(cardId))
                .findFirst();

        if (addressOptional.isEmpty() || cardOptional.isEmpty()){
            return ResponseEntity.notFound().build();
        }

        newOrder.setFullSendingAddress(addressOptional.get().toString());
        PaymentCard card = cardOptional.get();
        newOrder.setCardNumberEnding(card.getNumber().substring(card.getNumber().length() - 4));
        Order savedOrder = orderService.save(newOrder);

        //Send email confirmation
        // 2. ENVIAR EMAIL (Fuego y olvido)
        // Al ser @Async, esta línea no bloquea el retorno
        emailService.sendOrderConfirmation(
                loggedUser.getEmail(),
                loggedUser.getName(),
                savedOrder.getReferenceCode(),
                savedOrder.getItems(),
                savedOrder.getTotalCost()
        );


        return ResponseEntity.ok(new OrderDTO(savedOrder));
    }


    @GetMapping("/cart/summary")
    public ResponseEntity<CartSummaryDTO> getCartSummary(HttpServletRequest request) {
        //Get logged user info if any (User class)
        Optional<User> userOptional = userService.getLoggedUser(request);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        List<OrderItem> cartItems = orderItemService.findUserCartItemsList(loggedUser.getId());

        int totalItems = 0;
        double subtotal = 0.0;
        double totalDiscount = 0.0;
        double total = 0.0;

        for (OrderItem item : cartItems) {
            Product p = item.getProduct();
            int quantity = item.getQuantity();
            totalItems += quantity;

            double currentPrice = p.getCurrentPrice();
            double previousPrice = p.getPreviousPrice();

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
    @GetMapping("/cart")
    public ResponseEntity<OrderItemsPageDTO> getCartItemsPage(HttpServletRequest request, Pageable pageable) {
        //Get logged user info if any (User class)
        Optional<User> userOptional = userService.getLoggedUser(request);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        Page<OrderItem> cartItems = orderItemService.findUserCartItemsPage(loggedUser.getId(), pageable);
        return ResponseEntity.ok(toOrderItemPageDTO(cartItems));
    }

    @DeleteMapping("/cart")
    public ResponseEntity<CartSummaryDTO> clearCartItems(HttpServletRequest request) {
        //Get logged user info if any (User class)
        Optional<User> userOptional = userService.getLoggedUser(request);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        List<OrderItem> itemsToRemove = loggedUser.getItemsInCart();

        if (!itemsToRemove.isEmpty()) {
            loggedUser.getAllOrderItems().removeAll(itemsToRemove);
            userService.save(loggedUser);
        }

        return this.getCartSummary(request);
    }


    @PostMapping("/cart/{id}")
    public ResponseEntity<OrderItemDTO> addItemToCart(HttpServletRequest request,
                                                       @PathVariable Long id,
                                                       @RequestParam int quantity) {
        //Get logged user info if any (User class)
        Optional<User> userOptional = userService.getLoggedUser(request);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        //Check the product exists
        Optional<Product> productOptional = productService.findById(id);
        if(productOptional.isEmpty()){
            return ResponseEntity.notFound().build();
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
            return ResponseEntity.status(405).build(); //Not allowed, as there is not enough stock. Code linked in frontend
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

    @PutMapping("/cart/{id}")
    public ResponseEntity<CartSummaryDTO> updateItemQuantity(HttpServletRequest request,
                                                           @PathVariable Long id,
                                                           @RequestParam int quantity) {
        // 1. Get user
        Optional<User> userOptional = userService.getLoggedUser(request);
        if (userOptional.isEmpty()) return ResponseEntity.status(401).build();
        User loggedUser = userOptional.get();

        // 2. Get the cart item (needed to know how many items of this product does the user already have)
        Optional<OrderItem> itemInCartOptional = loggedUser.getItemsInCart().stream()
                .filter(item -> item.getProduct().getId().equals(id))
                .findFirst();

        if (itemInCartOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        OrderItem itemToUpdate = itemInCartOptional.get();

        // 3. Get product (to check total stock)
        Product product = itemToUpdate.getProduct();
        int totalStock = product.getShopsStock().stream().mapToInt(ShopStock::getStock).sum();

        // 4. Get all carts item units
        int totalInAllCarts = orderItemService.findProductUnitsInCart(id).stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        // 5. Limits
        // Free stock = Total stock - Stock in carts
        int freeStock = totalStock - totalInAllCarts;

        // Maximum items for this user: Current units + Free units
        int maxAchievableQuantity = itemToUpdate.getQuantity() + freeStock;

        // CASE A: Quantity is bigger than available
        if (quantity > maxAchievableQuantity) {
            quantity = maxAchievableQuantity; // Maximum available units
        }
        else if (quantity < 0) {
            if(itemToUpdate.getQuantity() > 0 || freeStock > 0){
                quantity = 1;
            }
            else{
                quantity = 0;
            }
        }

        // Validation: If after adjustments quantity is 0, throw an error
        if (quantity == 0) {
            return ResponseEntity.status(405).build();
        }

        // 6. Directly update quantity
        itemToUpdate.setQuantity(quantity);
        userService.save(loggedUser);

        return this.getCartSummary(request);
    }

    @DeleteMapping("/cart/{id}")
    public ResponseEntity<CartSummaryDTO> deleteCartItem(HttpServletRequest request, @PathVariable Long id) {
        //Get logged user info if any (User class)
        Optional<User> userOptional = userService.getLoggedUser(request);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        List<OrderItem> orderItems = loggedUser.getAllOrderItems();
        boolean removed = orderItems.removeIf(i ->
            i.getOrder() == null &&
            i.getProduct() != null &&
            i.getProduct().getId() != null &&
            i.getId().equals(id)
        );

        userService.save(loggedUser);

        if(!removed){
            return ResponseEntity.notFound().build();
        }
        return this.getCartSummary(request);
    }


    //Creates OrderItemsPageDTO objects with necessary fields only
    private OrderItemsPageDTO toOrderItemPageDTO(Page<OrderItem> items){
        List<OrderItemDTO> dtos = new ArrayList<>();
        for (OrderItem i : items.getContent()) {
            OrderItemDTO dto = new OrderItemDTO(i);
            dtos.add(dto);
        }
        return new OrderItemsPageDTO(dtos, items.getTotalElements(), items.getNumber(), items.getTotalPages()-1, items.getSize());
    }
}

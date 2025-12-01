package com.tfg.backend.controller;

import com.tfg.backend.DTO.OrderItemDTO;
import com.tfg.backend.DTO.OrderItemsPageDTO;
import com.tfg.backend.model.OrderItem;
import com.tfg.backend.model.Product;
import com.tfg.backend.model.ShopStock;
import com.tfg.backend.model.User;
import com.tfg.backend.service.OrderItemService;
import com.tfg.backend.service.ProductService;
import com.tfg.backend.service.UserService;
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

    

    //Custom method to achieve retrieving the order items from a list as a page given by the repository
    //Cart items of a user: items which order_id in DB is null and user_id is the same as the logged user id
    @GetMapping("/cart")
    public ResponseEntity<OrderItemsPageDTO> getCartItems(HttpServletRequest request, Pageable pageable) {
        //Get logged user info if any (User class)
        Optional<User> userOptional = userService.getLoggedUser(request);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        Page<OrderItem> cartItems = orderItemService.findUserCartItemsPage(loggedUser.getId(), pageable);
        return ResponseEntity.ok(toOrderItemPageDTO(cartItems));
    }

    @GetMapping("/cart/count")
    public ResponseEntity<Integer> countCartItems(HttpServletRequest request, Pageable pageable) {
        //Get logged user info if any (User class)
        Optional<User> userOptional = userService.getLoggedUser(request);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();

        Page<OrderItem> cartItems = orderItemService.findUserCartItemsPage(loggedUser.getId(), pageable);
        int totalItems = 0;
        for (OrderItem i : cartItems) {
            totalItems += i.getQuantity();
        }
        return ResponseEntity.ok(totalItems);
    }

    @DeleteMapping("/cart")
    public ResponseEntity<Void> clearCartItems(HttpServletRequest request, Pageable pageable) {
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

        return ResponseEntity.ok().build();
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
            resultItem = new OrderItem(null, productOptional.get(), loggedUser, quantity);
            loggedUser.getAllOrderItems().add(resultItem);
        }

        userService.save(loggedUser);
        return ResponseEntity.ok(new OrderItemDTO(resultItem)); //Returns the added item (optional)
    }

    @PutMapping("/cart/{id}")
    public ResponseEntity<OrderItemDTO> updateItemQuantity(HttpServletRequest request,
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
        // CASE B: Negative quantity
        else if (quantity < 0) {
            quantity = 0;
        }

        // Validation: If after adjustments quantity is 0, throw an error
        if (quantity == 0) {
            return ResponseEntity.status(405).build();
        }

        // 6. Directly update quantity
        itemToUpdate.setQuantity(quantity);
        userService.save(loggedUser);

        return ResponseEntity.ok(new OrderItemDTO(itemToUpdate));
    }

    @DeleteMapping("/cart/{id}")
    public ResponseEntity<Void> deleteCartItem(HttpServletRequest request, @PathVariable Long id) {
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

        return ResponseEntity.ok().build();
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

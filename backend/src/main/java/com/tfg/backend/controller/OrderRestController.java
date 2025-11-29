package com.tfg.backend.controller;

import com.tfg.backend.DTO.OrderItemDTO;
import com.tfg.backend.DTO.OrderItemsPageDTO;
import com.tfg.backend.model.OrderItem;
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
    public ResponseEntity<OrderItemsPageDTO> getCartProducts(HttpServletRequest request, Pageable pageable) {
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
    public ResponseEntity<Void> clearCartProducts(HttpServletRequest request, Pageable pageable) {
        //Get logged user info if any (User class)
        Optional<User> userOptional = userService.getLoggedUser(request);
        if(userOptional.isEmpty()){
            return ResponseEntity.status(401).build(); //Unauthorized as not logged
        }
        User loggedUser = userOptional.get();
        loggedUser.getAllOrderItems().clear();
        userService.save(loggedUser);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/cart/{id}")
    public ResponseEntity<Void> deleteCartProduct(HttpServletRequest request, @PathVariable Long id) {
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
